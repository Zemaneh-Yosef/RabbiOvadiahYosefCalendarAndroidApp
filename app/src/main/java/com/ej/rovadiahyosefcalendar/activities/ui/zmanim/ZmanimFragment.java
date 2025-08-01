package com.ej.rovadiahyosefcalendar.activities.ui.zmanim;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
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
import static com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableDark;
import static com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableLight;
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
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ej.rovadiahyosefcalendar.BuildConfig;
import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity;
import com.ej.rovadiahyosefcalendar.activities.JerusalemDirectionMapsActivity;
import com.ej.rovadiahyosefcalendar.activities.MoladActivity;
import com.ej.rovadiahyosefcalendar.activities.NetzActivity;
import com.ej.rovadiahyosefcalendar.activities.SettingsActivity;
import com.ej.rovadiahyosefcalendar.activities.SetupElevationActivity;
import com.ej.rovadiahyosefcalendar.activities.WelcomeScreenActivity;
import com.ej.rovadiahyosefcalendar.classes.ChaiTables;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesScraper;
import com.ej.rovadiahyosefcalendar.classes.DummyZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.ZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.ej.rovadiahyosefcalendar.databinding.FragmentZmanimBinding;
import com.ej.rovadiahyosefcalendar.notifications.DailyNotifications;
import com.ej.rovadiahyosefcalendar.notifications.NotificationUtils;
import com.ej.rovadiahyosefcalendar.notifications.OmerNotifications;
import com.ej.rovadiahyosefcalendar.notifications.ZmanimNotifications;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.TefilaRules;
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
    private NestedScrollView mNestedScrollView;
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
    private SharedPreferences.OnSharedPreferenceChangeListener sSharedPrefListener;
    private TextView mDailyLocationName;

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
        sSharedPrefListener = (prefs, key) -> {
            if (key != null && key.equals("isSetup")) {
                initMainView();
            }
        };
        sSharedPreferences.registerOnSharedPreferenceChangeListener(sSharedPrefListener);

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
                    mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                    mLocationResolver.setTimeZoneID();
                    resolveElevationAndVisibleSunrise(() -> {
                        instantiateZmanimCalendar();
                        setNextUpcomingZman();
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                        }
                        checkIfUserIsInIsraelOrNot();
                        setNotifications();
                        Utils.PrefToWatchSender.send(mContext);
                    });
                }
            }
        }

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupButtons();
    }

    private void initNotifResult() {
        sNotificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> setNotifications()
        );
    }

    /**
     * This method initializes the main view.
     */
    private void initMainView() {
        if (sLatitude == 0 && sLongitude == 0) {
            mLocationResolver.acquireLatitudeAndLongitude(this);
        }
        mLocationResolver.setTimeZoneID();
        if (binding != null) {
            setupDailyViews();
            hideWeeklyTextViews();
            binding.swipeRefreshLayout.setVisibility(View.GONE);
            mMainRecyclerView.setVisibility(View.GONE);
            binding.shimmerLayout.setVisibility(View.VISIBLE);
        }
        // hide everything except the shimmer layout before trying to see if we need to get elevation data
        if (sLatitude != 0 && sLongitude != 0) {// the values are updated, the accept method will not be called
            resolveElevationAndVisibleSunrise(() -> {
                instantiateZmanimCalendar();
                setNextUpcomingZman();
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    showWeeklyTextViews();
                    updateWeeklyZmanim();
                } else {
                    hideWeeklyTextViews();
                    updateDailyZmanim();
                }
                binding.shimmerLayout.setVisibility(View.GONE);
                createBackgroundThreadForNextUpcomingZman();
            });
        }
        setupButtons();
        setNotifications();
        askForRealTimeNotificationPermissions();
        checkIfUserIsInIsraelOrNot();
    }

    private void setupButtons() {
        setupDayHopButtons(true);
        setupCalendarCardAndButton();
        setupDayHopButtons(false);
    }

    /**
     * Sets up the previous day button
     */
    private void setupDayHopButtons(boolean previous) {
        if (binding == null)
            return;

        Button dateBind = (previous ? binding.prevDay : binding.nextDay);
        dateBind.setOnClickListener(v -> {
            if (!sShabbatMode) {
                mCurrentDateShown = (Calendar) mROZmanimCalendar.getCalendar().clone();//just get a calendar object with the same date as the current one

                int dateChange = (sSharedPreferences.getBoolean("weeklyMode", false) ? 7 : 1) * (previous ? -1 : 1);
                mCurrentDateShown.add(Calendar.DATE, dateChange);

                mROZmanimCalendar.setCalendar(mCurrentDateShown);
                mJewishDateInfo.setCalendar(mCurrentDateShown);
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                seeIfTablesNeedToBeUpdated(true);
            }
        });

        if (previous)
            mPreviousDate = dateBind;
        else
            mNextDate = dateBind;
    }

    /**
     * Setup the calendar button to show a DatePickerDialog with an additional button to switch the calendar to the hebrew one.
     */
    private void setupCalendarCardAndButton() {
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
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
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
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
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

            binding.dailyCard.setOnClickListener(l -> {
                if (mCalendarButton != null) {
                    mCalendarButton.performClick();
                }
            });

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        }
    }

    /**
     * This method will automatically update the tables if the user has setup the app before for the current location.
     *
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
                            scraper.setCallback(() -> {
                                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                    updateWeeklyZmanim();
                                } else {
                                    updateDailyZmanim();
                                }
                            });
                            scraper.start();
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

    private void setupDailyViews() {
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            Looper.prepare();
            if (mLocationResolver == null) {
                mLocationResolver = new LocationResolver(mContext, mActivity);
            }
            mLocationResolver.acquireLatitudeAndLongitude(this);
            mLocationResolver.setTimeZoneID();
            if (mCurrentDateShown != null && mROZmanimCalendar != null && mMainRecyclerView != null) {
                mCurrentDateShown.setTime(new Date());
                mJewishDateInfo.setCalendar(new GregorianCalendar());
                resolveElevationAndVisibleSunrise(() -> {
                    instantiateZmanimCalendar();
                    setNextUpcomingZman();
                    sSharedPreferences.edit().putString("Full" + mROZmanimCalendar.getGeoLocation().getLocationName(), "").apply();
                    updateDailyZmanim();
                    mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                });
            }
            swipeRefreshLayout.setRefreshing(false);
            Objects.requireNonNull(Looper.myLooper()).quit();
        }).start());
        mNestedScrollView = binding.nestedScrollView;
        mDailyLocationName = binding.dailyLocationName;
        mDailyLocationName.setPaintFlags(mDailyLocationName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        mDailyLocationName.setOnClickListener(v -> new MaterialAlertDialogBuilder(mContext)
                .setTitle(mContext.getString(R.string.location_info_for) + " " + sSharedPreferences.getString("Full" + mROZmanimCalendar.getGeoLocation().getLocationName(), ""))
                .setMessage(mContext.getString(R.string.location_name) + " " + sCurrentLocationName + "\n" +
                        mContext.getString(R.string.latitude) + " " + sLatitude + "\n" +
                        mContext.getString(R.string.longitude) + " " + sLongitude + "\n" +
                        mContext.getString(R.string.elevation) + " " +
                        (sSharedPreferences.getBoolean("useElevation", true) ?
                                sSharedPreferences.getString("elevation" + sCurrentLocationName, "0") : "0")
                        + " " + mContext.getString(R.string.meters) + "\n" +
                        mContext.getString(R.string.time_zone) + sCurrentTimeZoneID)
                .setPositiveButton(R.string.share, ((dialog, which) -> {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "https://royzmanim.com/calendar?locationName=" + sCurrentLocationName.replace(" ", "+").replace(",", "%2C") + "&lat=" + sLatitude + "&long=" + sLongitude + "&elevation=" + sElevation + "&timeZone=" + sCurrentTimeZoneID);
                    mContext.startActivity(Intent.createChooser(sendIntent, mContext.getString(R.string.share)));
                }))
                .setNeutralButton(R.string.change_location, (dialog, which) -> mContext.startActivity(new Intent(mContext, GetUserLocationWithMapActivity.class).putExtra("loneActivity", true)))
                .setNegativeButton(mContext.getString(R.string.setup_elevation), (dialog, which) -> mContext.startActivity(new Intent(mContext, SetupElevationActivity.class).putExtra("loneActivity", true)))
                .show());
        binding.dailyCard.setCardElevation(12f);
        binding.parshaCard.setCardElevation(12f);
        binding.parsha.setOnClickListener(l -> {
            if (binding != null) {
                String title = binding.parsha.getText().toString();
                if (!title.equals("No Weekly Parsha") && !title.equals("אין פרשת שבוע")) {
                    String parsha;
                    if (title.equals("לך לך")
                            || title.equals("חיי שרה")
                            || title.equals("כי תשא")
                            || title.equals("אחרי מות")
                            || title.equals("שלח לך")
                            || title.equals("כי תצא")
                            || title.equals("כי תבוא")
                            || title.equals("וזאת הברכה ")) {
                        parsha = title;// ugly, but leave the first word and second word in these cases
                    } else {
                        if (title.contains("אחרי מות")) {// edge case for Acharei Mot Kedoshim
                            parsha = "אחרי מות";
                        } else {
                            parsha = title.split(" ")[0];//get first word
                        }
                    }
                    String parshaLink = "https://www.sefaria.org/" + parsha;
                    MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext);
                    dialogBuilder.setTitle(mContext.getString(R.string.open_sefaria_link_for) + parsha + "?");
                    dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_weekly_parsha);
                    dialogBuilder.setPositiveButton(mContext.getString(R.string.ok), (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(parshaLink));
                        mContext.startActivity(intent);
                    });
                    dialogBuilder.setNegativeButton(mContext.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                }
            }
        });
        mMainRecyclerView = binding.mainRV;
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            mMainRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        RecyclerView dummyRecyclerView = binding.dummyRV;
        dummyRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        dummyRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        dummyRecyclerView.setAdapter(new DummyZmanAdapter(30));
    }

    private void checkIfUserIsInIsraelOrNot() {
        if (sSharedPreferences.getBoolean("neverAskInIsraelOrNot", false)) {
            return;
        }

        if (Utils.isInOrNearIsrael(sLatitude, sLongitude)) {//user is in or near israel now
            if (!sSharedPreferences.getBoolean("inIsrael", false)) {//user was not in israel before
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.are_u_in_israel)
                        .setMessage(R.string.if_you_are_in_israel_now_please_confirm_below)
                        .setPositiveButton(R.string.yes_i_am_in_israel, (dialog, which) -> {
                            mJewishDateInfo.getJewishCalendar().setInIsrael(true);
                            sSharedPreferences.edit().putBoolean("inIsrael", true).apply();
                            sSharedPreferences.edit().putBoolean("useElevation", true).apply();
                            sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", false).apply();
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            instantiateZmanimCalendar();
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                        })
                        .setNegativeButton(R.string.no_i_am_not_in_israel, (dialog, which) -> dialog.dismiss())
                        .setNeutralButton(R.string.do_not_ask_me_again, (dialog, which) -> {
                            sSharedPreferences.edit().putBoolean("neverAskInIsraelOrNot", true).apply();//save that we should never ask again
                            dialog.dismiss();
                        })
                        .show();
            }
        } else {//user is not in israel
            if (sSharedPreferences.getBoolean("inIsrael", false)) {//user was in israel before
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.have_you_left_israel)
                        .setMessage(R.string.if_you_are_not_in_israel_now_please_confirm_below_otherwise_ignore_this_message)
                        .setPositiveButton(R.string.yes_i_have_left_israel, (dialog, which) -> {
                            mJewishDateInfo.getJewishCalendar().setInIsrael(false);
                            sSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                            sSharedPreferences.edit().putBoolean("useElevation", false).apply();
                            sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", true).apply();
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            instantiateZmanimCalendar();
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                        })
                        .setNegativeButton(R.string.no_i_have_not_left_israel, (dialog, which) -> dialog.dismiss())
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
        mNestedScrollView.setVisibility(View.GONE);
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
        mNestedScrollView.setVisibility(View.VISIBLE);
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
            mROZmanimCalendar.setAteretTorahSunsetOffset(sSharedPreferences.getBoolean("inIsrael", false) ? 30 : 40);
        } else {
            mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
        }
        if (sSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
            mROZmanimCalendar.setAteretTorahSunsetOffset(30);
        }
        mROZmanimCalendar.setAmudehHoraah(sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false));
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
                } else {
                    if (!sSharedPreferences.getBoolean("hasShownVSNotification", false)) {
                        Utils.showVisibleSunriseNotification(mContext);
                        sSharedPreferences.edit().putBoolean("hasShownVSNotification", true).apply();
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) mContext.getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {// more annoying android permission garbage
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
                builder.setTitle(R.string.zmanim_notifications_will_not_work);
                builder.setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications);
                builder.setCancelable(false);
                builder.setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> sNotificationLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + mContext.getPackageName()))));
                builder.setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss());
                if (!mActivity.isFinishing()) {
                    builder.show();
                }
            }

            if (!sSharedPreferences.getBoolean("neverAskBatteryOptimization", false)) {
                PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                if (!powerManager.isIgnoringBatteryOptimizations(mContext.getPackageName())) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
                    builder.setTitle(R.string.battery_optimization_is_enabled);
                    builder.setMessage(R.string.the_current_battery_settings_for_the_app_is_trying_to_save_battery_this_may_cause_notifications_to_be_sent_at_a_later_time_would_you_like_to_change_this_setting);
                    builder.setCancelable(false);
                    builder.setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> mContext.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
                    builder.setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss());
                    builder.setNeutralButton(mContext.getString(R.string.do_not_ask_me_again), (dialog, which) -> {
                        sSharedPreferences.edit().putBoolean("neverAskBatteryOptimization", true).apply();
                        dialog.dismiss();
                    });
                    if (!mActivity.isFinishing()) {
                        builder.show();
                    }
                }
            }
        }
        setAllNotifications();
    }

    private void setAllNotifications() {
        if (mROZmanimCalendar.getGeoLocation().equals(new GeoLocation())) {
            return; // DO NOT SET ANY NOTIFICATIONS WITH A DEFAULT LOCATION
        }
        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Geolocation for notifications = " + mROZmanimCalendar.getGeoLocation().toString() + "\n\n").apply();
        }
        Calendar calendar = Calendar.getInstance();
        ROZmanimCalendar roZmanimCalendar = mROZmanimCalendar.getCopy();
        roZmanimCalendar.setCalendar(Calendar.getInstance()); // do this in order to always set the notifications on the current date
        Date sunrise = roZmanimCalendar.getSunrise();
        if (sunrise == null) {
            sunrise = new Date();
        }
        calendar.setTimeInMillis(sunrise.getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
        if (BuildConfig.DEBUG) {
            if (sSharedPreferences.getString("debugNotifs", "").length() > 10000) {
                sSharedPreferences.edit().putString("debugNotifs", "").apply();
            }
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Daily Notifications set for: " + calendar.getTime() + "\n\n").apply();
        }
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.getTimeInMillis(), dailyPendingIntent);

        Date tzeit = roZmanimCalendar.getTzeit();
        if (tzeit == null) {
            tzeit = new Date();
        }
        calendar.setTimeInMillis(tzeit.getTime());
        if (calendar.getTime().compareTo(new Date()) < 0) {
            calendar.add(Calendar.DATE, 1);
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE);
        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Omer Notifications set for: " + calendar.getTime() + "\n\n").apply();
        }
        NotificationUtils.setExactAndAllowWhileIdle(am, calendar.getTimeInMillis(), omerPendingIntent);

        Intent zmanIntent = new Intent(mContext, ZmanimNotifications.class);
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(mContext, 0, zmanIntent, PendingIntent.FLAG_IMMUTABLE);
        try {
            zmanimPendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateDailyZmanim() {
        if (mMainRecyclerView == null) {
            if (binding != null) {
                mMainRecyclerView = binding.mainRV;
                updateDailyZmanim();// try again
            }
        } else {
            mMainRecyclerView.setAdapter(new ZmanAdapter(mContext, getZmanimList(false),
                    () -> {
                        ZmanAdapter zmanAdapter = (ZmanAdapter) mMainRecyclerView.getAdapter();
                        if (zmanAdapter != null) {
                            zmanAdapter.setZmanim(getZmanimList(true));
                            mMainRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }));
        }
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        if (Utils.isLocaleHebrew()) {
            materialToolbar.setSubtitle("");
        } else {
            materialToolbar.setTitle(mContext.getString(R.string.app_name));
        }
        materialToolbar.getMenu().clear();
        materialToolbar.inflateMenu(R.menu.menu_main);
        materialToolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.search_for_a_place) {
                startActivity(new Intent(mContext, GetUserLocationWithMapActivity.class).putExtra("loneActivity", true));
                return true;
            } else if (itemId == R.id.shabbat_mode) {
                if (!sShabbatMode && mROZmanimCalendar != null && mMainRecyclerView != null) {
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
            } else if (itemId == R.id.weekly_mode) {
                sSharedPreferences.edit().putBoolean("weeklyMode", !sSharedPreferences.getBoolean("weeklyMode", false)).apply();
                item.setChecked(sSharedPreferences.getBoolean("weeklyMode", false));
                if (mMainRecyclerView == null || binding == null) {
                    return true;
                }
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    showWeeklyTextViews();
                    updateWeeklyZmanim();
                } else {
                    hideWeeklyTextViews();
                    updateDailyZmanim();
                }
                return true;
            } else if (itemId == R.id.use_elevation) {
                sSharedPreferences.edit().putBoolean("useElevation", !sSharedPreferences.getBoolean("useElevation", false)).apply();
                item.setChecked(sSharedPreferences.getBoolean("useElevation", false));
                resolveElevationAndVisibleSunrise(() -> {
                    instantiateZmanimCalendar();
                    setNextUpcomingZman();
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                    }
                });
                return true;
            } else if (itemId == R.id.jerDirection) {
                startActivity(new Intent(mContext, JerusalemDirectionMapsActivity.class));
                return true;
            } else if (itemId == R.id.netzView) {
                startActivity(new Intent(mContext, NetzActivity.class));
                return true;
            } else if (itemId == R.id.molad) {
                startActivity(new Intent(mContext, MoladActivity.class));
                return true;
            } else if (itemId == R.id.fullSetup) {
                sSetupLauncher.launch(new Intent(mContext, WelcomeScreenActivity.class));
                return true;
            } else if (itemId == R.id.settings) {
                startActivity(new Intent(mContext, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.website) {
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
    }

    /**
     * This is the main method for updating the Zmanim in the list view. It is called everytime the user changes the date or updates
     * any setting that affects the zmanim. This method returns a list of ZmanListEntry objects that will be used to populate the list view.
     *
     * @return List of ZmanListEntry objects that will be used to populate the list view.
     * @see ZmanListEntry
     */
    private List<ZmanListEntry> getZmanimList(boolean add66MisheyakirZman) {
        List<ZmanListEntry> zmanim = new ArrayList<>();

        String locationName = sSharedPreferences.getString("Full" + mROZmanimCalendar.getGeoLocation().getLocationName(), "");
        if (locationName.isEmpty()) {
            locationName = mLocationResolver.getFullLocationName();
            sSharedPreferences.edit().putString("Full" + mROZmanimCalendar.getGeoLocation().getLocationName(), locationName).apply();
        }
        if (locationName != null && locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
            locationName = mROZmanimCalendar.getGeoLocation().getLocationName();
        }

        String engDate = mROZmanimCalendar.getCalendar().get(Calendar.DATE) +
                " " +
                mROZmanimCalendar.getCalendar().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) +
                ", " +
                mROZmanimCalendar.getCalendar().get(Calendar.YEAR);

        if (binding != null) {
            binding.dailyLocationName.setText(locationName);
            if (DateUtils.isSameDay(mROZmanimCalendar.getCalendar().getTime(), new Date())) {
                binding.dailyCard.setStrokeColor(ContextCompat.getColor(mContext, R.color.sunset_orange));
                binding.dailyCard.setStrokeWidth(6);

                engDate = mContext.getString(R.string.today) + " — " + engDate;
                String hebDate;
                if (mROZmanimCalendar.getSunset() != null && new Date().after(mROZmanimCalendar.getSunset())) {
                    hebDate = mContext.getString(R.string.post_sunset_date);
                } else {
                    hebDate = mContext.getString(R.string.pre_sunset_date);
                }
                hebDate += " " + mJewishDateInfo.getJewishCalendar().currentToString(mROZmanimCalendar);
                binding.gregDate.setText(engDate);
                binding.hebDate.setText(hebDate);
            } else {
                binding.dailyCard.setStrokeColor(ContextCompat.getColor(mContext, R.color.cardview_border));
                binding.dailyCard.setStrokeWidth(3);
                binding.gregDate.setText(engDate);
                binding.hebDate.setText(mJewishDateInfo.getJewishCalendar().toString());
            }
            binding.weekday.setText(Utils.isLocaleHebrew() ?
                    mROZmanimCalendar.getCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    :
                    mROZmanimCalendar.getCalendar()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    + " / " +
                    mROZmanimCalendar.getCalendar().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, new Locale("he", "IL")));
            binding.parsha.setText(mJewishDateInfo.getThisWeeksParsha());
            binding.haftara.setText(mJewishDateInfo.getThisWeeksHaftarah());
        }

        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", false)) {
            if (mJewishDateInfo.tomorrow().getJewishCalendar().isShabbosMevorchim()) {
                zmanim.add(new ZmanListEntry("שבת מברכים"));
            }
        }

        String day = mJewishDateInfo.getSpecialDay(false);
        if (!day.isEmpty()) {
            zmanim.add(new ZmanListEntry(day));
        }
        String dayOfOmer = mJewishDateInfo.addDayOfOmer("");
        if (!dayOfOmer.isEmpty()) {
            zmanim.add(new ZmanListEntry(dayOfOmer));
        }

        if (mJewishDateInfo.getJewishCalendar().isRoshHashana() && mJewishDateInfo.isShmitaYear()) {
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

        if (mJewishDateInfo.isPurimMeshulash()) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.no_tachanun_in_yerushalayim)));
        }

        String birchatLevana = mJewishDateInfo.getBirchatLevana();
        if (!birchatLevana.isEmpty()) {
            zmanim.add(new ZmanListEntry(birchatLevana));
            MoonTimes moonTimes = MoonTimes.compute()
                    .on(mCurrentDateShown.getTime())
                    .at(sLatitude, sLongitude)
                    .timezone(sCurrentTimeZoneID == null ? TimeZone.getDefault().getID() : sCurrentTimeZoneID)
                    .midnight()
                    .execute();
            if (moonTimes.isAlwaysUp()) {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.the_moon_is_up_all_night)));
            } else if (moonTimes.isAlwaysDown()) {
                zmanim.add(new ZmanListEntry(mContext.getString(R.string.there_is_no_moon_tonight)));
            } else {
                SimpleDateFormat moonFormat;
                if (Utils.isLocaleHebrew()) {
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

        if (mJewishDateInfo.tomorrow().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                && mJewishDateInfo.tomorrow().getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            zmanim.add(new ZmanListEntry(mContext.getString(R.string.burn_your_ametz_today)));
        }

        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        switch (tekufaOpinions) {
            case "1":
                if (sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                    addAmudeiHoraahTekufaTime(zmanim, false);
                } else {
                    addTekufaTime(zmanim, false);
                }
                break;
            case "2":
                addTekufaTime(zmanim, false);
                break;
            case "3":
                addAmudeiHoraahTekufaTime(zmanim, false);
                break;
            default://I.E. 4
                addAmudeiHoraahTekufaTime(zmanim, false);
                addTekufaTime(zmanim, false);
                break;
        }
        addTekufaLength(zmanim, tekufaOpinions);

        addZmanim(zmanim, false, sSettingsPreferences, sSharedPreferences, mROZmanimCalendar, mJewishDateInfo, add66MisheyakirZman);

        zmanim.add(new ZmanListEntry(mJewishDateInfo.getIsMashivHaruchOrMoridHatalSaid()
                + " / "
                + mJewishDateInfo.getIsBarcheinuOrBarechAleinuSaid()));

        zmanim.add(new ZmanListEntry(mContext.getString(R.string.shaah_zmanit_gr_a) + " " + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanisGra())));
        zmanim.add(new ZmanListEntry(mContext.getString(R.string.mg_a)
                + " (" + mContext.getString(mROZmanimCalendar.isUseAmudehHoraah() ? R.string.amudei_horaah : R.string.ohr_hachaim) + ") "
                + mZmanimFormatter.format(mROZmanimCalendar.getShaahZmanis72MinutesZmanis())));

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
            switch (mJewishDateInfo.getYearOfShmitaCycle()) {
                case 1:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.first_year_of_shmita)));
                    break;
                case 2:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.second_year_of_shmita)));
                    break;
                case 3:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.third_year_of_shmita)));
                    break;
                case 4:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.fourth_year_of_shmita)));
                    break;
                case 5:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.fifth_year_of_shmita)));
                    break;
                case 6:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.sixth_year_of_shmita)));
                    break;
                default:
                    zmanim.add(new ZmanListEntry(mContext.getString(R.string.this_year_is_a_shmita_year)));
                    break;
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
            mHandler.postDelayed(nextZmanUpdater, sNextUpcomingZman.getTime() - new Date().getTime() + 1_000);//add 1 second to make sure we don't get the same zman again
        }
    }

    public void setNextUpcomingZman() {
        ZmanListEntry nextZman = ZmanimFactory.getNextUpcomingZman(mCurrentDateShown, mROZmanimCalendar, mJewishDateInfo, sSettingsPreferences, sSharedPreferences);
        if (nextZman == null || nextZman.getZman() == null) {
            nextZman = new ZmanListEntry("", new Date(System.currentTimeMillis() + 30_000), SecondTreatment.ROUND_EARLIER, "");// try again in 30 seconds
        }
        sNextUpcomingZman = nextZman.getZman();
    }

    private String getAnnouncements() {
        StringBuilder announcements = new StringBuilder();

        String day = mJewishDateInfo.getSpecialDay(true);
        if (!day.isEmpty()) {
            announcements.append(day.replace("/ ", "\n")).append("\n");
        }

        if (mJewishDateInfo.isPurimMeshulash()) {
            announcements.append(mContext.getString(R.string.no_tachanun_in_yerushalayim));
        }

        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (mJewishDateInfo.tomorrow().getJewishCalendar().isShabbosMevorchim()) {
                announcements.append("שבת מברכים").append("\n");
            }
        }

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

        if (mJewishDateInfo.tomorrow().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                && mJewishDateInfo.tomorrow().getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            announcements.append(mContext.getString(R.string.burn_your_ametz_today));
        }

        List<ZmanListEntry> tekufa = new ArrayList<>();
        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        switch (tekufaOpinions) {
            case "1":
                if (sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                    addAmudeiHoraahTekufaTime(tekufa, true);
                } else {
                    addTekufaTime(tekufa, true);
                }
                break;
            case "2":
                addTekufaTime(tekufa, true);
                break;
            case "3":
                addAmudeiHoraahTekufaTime(tekufa, true);
                break;
            default:// 4
                addAmudeiHoraahTekufaTime(tekufa, true);
                addTekufaTime(tekufa, true);
                break;
        }
        if (!tekufa.isEmpty()) {
            for (ZmanListEntry tekufaEntry : tekufa) {
                announcements.append(tekufaEntry.getTitle()).append("\n");
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
        if (Utils.isLocaleHebrew()) {
            hebrewDateFormatter.setHebrewFormat(true);
        }
        List<TextView[]> weeklyInfo = Arrays.asList(mSunday, mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday);

        String month = mCurrentDateShown.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String year = String.valueOf(mCurrentDateShown.get(Calendar.YEAR));

        String hebrewMonth = hebrewDateFormatter.formatMonth(mJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet");
        String hebrewYear = String.valueOf(mJewishDateInfo.getJewishCalendar().getJewishYear());
        if (Utils.isLocaleHebrew()) {
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
            mListViews[i].setAdapter(new ArrayAdapter<>(mContext, R.layout.zman_list_view, getShortZmanim()) {
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
        if (Utils.isLocaleHebrew()) {
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
        if (Utils.isLocaleHebrew()) {
            mEnglishMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            mHebrewMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
        mLocationName.setText(mLocationResolver.getFullLocationName());
        String hebrewMonthYear = hebrewMonth + " " + hebrewYear;
        mHebrewMonthYear.setText(hebrewMonthYear);
        mWeeklyParsha.setText(mJewishDateInfo.getThisWeeksParsha());
        if (mJewishDateInfo.getThisWeeksHaftarah().isEmpty()) {
            mWeeklyHaftorah.setVisibility(View.GONE);
        } else {
            mWeeklyHaftorah.setVisibility(View.VISIBLE);
            mWeeklyHaftorah.setText(mJewishDateInfo.getThisWeeksHaftarah());
        }
        mROZmanimCalendar.getCalendar().setTimeInMillis(backupCal.getTimeInMillis());
        mJewishDateInfo.setCalendar(backupCal);
        mCurrentDateShown = backupCal;
    }

    private String[] getShortZmanim() {
        List<ZmanListEntry> zmanim = new ArrayList<>();
        addZmanim(zmanim, true, sSettingsPreferences, sSharedPreferences, mROZmanimCalendar, mJewishDateInfo, true);

        String dateFormatPattern = "H:mm" + (sSettingsPreferences.getBoolean("ShowSeconds", false) ? ":ss" : "");
        if (!Utils.isLocaleHebrew())
            dateFormatPattern = dateFormatPattern.toLowerCase() + " aa";
        DateFormat zmanimFormat = new SimpleDateFormat(dateFormatPattern, Locale.getDefault());
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        //filter out important zmanim
        List<ZmanListEntry> zmansToRemove = new ArrayList<>();
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    if (!Utils.isLocaleHebrew()) {
                        mZmanimForAnnouncements.add(zmanimFormat.format(zman.getZman()) + " :" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                    } else {
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanimFormat.format(zman.getZman()));
                    }
                    zmansToRemove.add(zman);
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanimFormat.format(zman.getZman()));
                    zmansToRemove.add(zman);
                }
            }
        }
        zmanim.removeAll(zmansToRemove);

        //here is where we actually create the list of zmanim to display
        String[] shortZmanim = new String[zmanim.size()];
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            for (ZmanListEntry zman : zmanim) {
                if (!Utils.isLocaleHebrew()) {
                    shortZmanim[zmanim.indexOf(zman)] = zmanimFormat.format(zman.getZman()) + " :" + zman.getTitle()
                            .replace("סוף זמן ", "")
                            .replace("(", "")
                            .replace(")", "");
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                            .replace("סוף זמן ", "")
                            .replace("(", "")
                            .replace(")", "") + ": " + zmanimFormat.format(zman.getZman());
                }

                if (zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] +
                            (Utils.isLocaleHebrew() ? " ➤ " : " ◄ ");
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                        .replace("Earliest ", "")
                        .replace("Sof Zeman ", "")
                        .replace("Latest ", "")
                        .replace("(", "")
                        .replace(")", "")
                        + ": " + zmanimFormat.format(zman.getZman());
                if (zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] +
                            (Utils.isLocaleHebrew() ? " ➤ " : " ◄ ");
                }
            }
        }
        return shortZmanim;
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
     *
     * @param zmanim     the list of zmanim to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     */
    private void addTekufaTime(List<ZmanListEntry> zmanim, boolean shortStyle) {
        DateFormat zmanimFormat;
        if (Utils.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        ROZmanimCalendar zmanimCalendarCopy = mROZmanimCalendar.getCopy();
        JewishDateInfo jewishDateInfoCopy = mJewishDateInfo.getCopy();
        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, 1);//check next day for tekufa, because the tekufa time can go back a day
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());
        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, -1);//reset the calendar to check for the current date
        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Utils.isLocaleHebrew()) {
                    zmanim.add(new ZmanListEntry("תקופת " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " היום בשעה ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " is today at ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate())));
                }
            }
        }
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Utils.isLocaleHebrew()) {
                    zmanim.add(new ZmanListEntry("תקופת " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " היום בשעה ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " is today at ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate())));
                }
            }
        }
    }

    /**
     * This method will check if the tekufa happens within the next 48 hours and it will add the tekufa to the list passed in if it happens
     * on the current date.
     *
     * @param zmanim     the list of zmanim to add to
     * @param shortStyle if the tekufa should be added as "Tekufa Nissan : 4:30" or "Tekufa Nissan is today at 4:30"
     */
    private void addAmudeiHoraahTekufaTime(List<ZmanListEntry> zmanim, boolean shortStyle) {
        DateFormat zmanimFormat;
        if (Utils.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
        ROZmanimCalendar zmanimCalendarCopy = mROZmanimCalendar.getCopy();
        JewishDateInfo jewishDateInfoCopy = mJewishDateInfo.getCopy();
        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, 1);//check next day for tekufa, because the tekufa time can go back a day
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());
        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, -1);//reset the calendar to check for the current date

        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Utils.isLocaleHebrew()) {
                    zmanim.add(new ZmanListEntry("תקופת " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " היום בשעה ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " is today at ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            }
        }
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {

                if (Utils.isLocaleHebrew()) {
                    zmanim.add(new ZmanListEntry("תקופת " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " היום בשעה ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                } else {
                    zmanim.add(new ZmanListEntry("Tekufa " + jewishDateInfoCopy.getJewishCalendar().getTekufaName() +
                            (shortStyle ? " : " : " is today at ") +
                            zmanimFormat.format(jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate())));
                }
            }
        }
    }

    public void addTekufaLength(List<ZmanListEntry> zmanim, String opinion) {
        DateFormat zmanimFormat;
        if (Utils.isLocaleHebrew()) {
            zmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            zmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        Date tekufa = null;
        Date aHTekufa = null;

        ROZmanimCalendar zmanimCalendarCopy = mROZmanimCalendar.getCopy();
        JewishDateInfo jewishDateInfoCopy = mJewishDateInfo.getCopy();

        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, 1);//check next day for tekufa, because the tekufa time can go back a day
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());
        zmanimCalendarCopy.getCalendar().add(Calendar.DATE, -1);//reset the calendar to check for the current date

        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                tekufa = jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate();
                aHTekufa = jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
            }
        }
        jewishDateInfoCopy.setCalendar(zmanimCalendarCopy.getCalendar());//reset

        //else the tekufa time is on the same day as the current date, so we can add it normally
        if (jewishDateInfoCopy.getJewishCalendar().getTekufa() != null) {

            final Calendar cal1 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            final Calendar cal2 = (Calendar) zmanimCalendarCopy.getCalendar().clone();
            cal2.setTime(jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate());// should not be null in this if block

            if (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)) {
                tekufa = jewishDateInfoCopy.getJewishCalendar().getTekufaAsDate();
                aHTekufa = jewishDateInfoCopy.getJewishCalendar().getAmudeiHoraahTekufaAsDate();
            }
        }

        if (tekufa != null && aHTekufa != null) {
            Date halfHourBefore;
            Date halfHourAfter;
            switch (opinion) {
                case "1":
                    if (sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false)) {
                        halfHourBefore = new Date(aHTekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                        halfHourAfter = new Date(aHTekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                    } else {
                        halfHourBefore = new Date(tekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                        halfHourAfter = new Date(tekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                    }
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                    } else {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                    }
                    break;
                case "2":
                    halfHourBefore = new Date(tekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                    halfHourAfter = new Date(tekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                    } else {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                    }
                    break;
                case "3":
                    halfHourBefore = new Date(aHTekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                    halfHourAfter = new Date(aHTekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                    } else {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                    }
                    break;
                default:// 4
                    halfHourBefore = new Date(aHTekufa.getTime() - (DateUtils.MILLIS_PER_HOUR / 2));
                    halfHourAfter = new Date(tekufa.getTime() + (DateUtils.MILLIS_PER_HOUR / 2));
                    if (Utils.isLocaleHebrew()) {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourAfter) + " - " + zmanimFormat.format(halfHourBefore)));
                    } else {
                        zmanim.add(new ZmanListEntry(mContext.getString(R.string.tekufa_length) + zmanimFormat.format(halfHourBefore) + " - " + zmanimFormat.format(halfHourAfter)));
                    }
                    break;
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
    private void resolveElevationAndVisibleSunrise(Runnable codeToRunOnMainThread) {
        boolean sUserIsOffline = false;
        if (sCurrentLocationName != null && !sCurrentLocationName.isEmpty()) {// Somehow it is null and crashing in some places
            if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")
                    && sSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false)) {//only if the user has enabled the setting to set the elevation to the last known location
                sUserIsOffline = true;
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
            } else {//user is online, get the elevation from the shared preferences for the current location
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));//get the last value of the current location or 0 if it doesn't exist
            }
        }

        if (!sUserIsOffline && sSharedPreferences.getBoolean("useElevation", true)) {//update if the user is online and the elevation setting is enabled
            if (!sSharedPreferences.contains("elevation" + sCurrentLocationName)) {//if the elevation for this location has never been set
                Thread thread = new Thread(() -> mLocationResolver.getElevationFromWebService(mHandler,
                        () -> sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0")),
                        codeToRunOnMainThread));
                thread.start();
                seeIfTablesNeedToBeUpdated(false);
            } else {// use elevation that was set before
                sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
                mActivity.runOnUiThread(codeToRunOnMainThread);
            }
        } else {// user does not want elevation
            sElevation = 0;
            mActivity.runOnUiThread(codeToRunOnMainThread);
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
     *
     * @see #startScrollingThread() to start the thread that will scroll through the list of zmanim
     * @see #setShabbatBannerColors(boolean) to set the text of the shabbat banners
     */
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
            Configuration configuration = getResources().getConfiguration();
            if (binding != null) {
                TextClock clock = binding.clock;
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    clock.setVisibility(View.VISIBLE);
                    if (Utils.isLocaleHebrew()) {
                        clock.setFormat24Hour("hh:mm:ss");
                    }
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                setShabbatBannerColors(false);
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                mHandler.removeCallbacks(mZmanimUpdater);
                mHandler.postDelayed(mZmanimUpdater, TWENTY_FOUR_HOURS_IN_MILLI);//run the update in 24 hours
            };
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 2);
            calendar.add(Calendar.DATE, 1);
            mHandler.postDelayed(mZmanimUpdater, calendar.getTimeInMillis() - calendar2.getTimeInMillis());//time remaining until 12:00:02am the next day
            startScrollingThread();
        }
    }

    /**
     * Sets the text of the shabbat banners based on the NEXT day's date, since most people will start shabbat mode before shabbat/chag starts.
     *
     * @param isFirstTime if true, the text will be set based on the next day's date, otherwise it will be set based on the current date.
     *                    Since it will be called at 12:00:02am the next day, we do not need to worry about the next day's date.
     */
    private void setShabbatBannerColors(boolean isFirstTime) {
        if (isFirstTime) {
            mCurrentDateShown.add(Calendar.DATE, 1);
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
                String def = mContext.getString(R.string.SHABBAT_MODE) +
                        "                " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE) +
                        "               " +
                        mContext.getString(R.string.SHABBAT_MODE);
                mShabbatModeBanner.setText(def);
                mShabbatModeBanner.setBackgroundColor(mContext.getColor(R.color.dark_blue));
                mShabbatModeBanner.setTextColor(mContext.getColor(R.color.white));
                mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_blue));
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(mCurrentDateShown));
        }

        if (isFirstTime) {
            mCurrentDateShown.add(Calendar.DATE, -1);
            mJewishDateInfo.setCalendar(mCurrentDateShown);
        }
    }

    /**
     * This method is called when the user clicks on shabbat mode. It will create another thread that will constantly try to scroll the recycler view
     * up and down.
     */
    @SuppressWarnings({"BusyWait"})
    private void startScrollingThread() {
        if (!sSharedPreferences.getBoolean("weeklyMode", false)) {
            Thread scrollingThread = new Thread(() -> {
                while (mNestedScrollView != null && mNestedScrollView.canScrollVertically(1)) {
                    if (!sShabbatMode) break;
                    if (mNestedScrollView.canScrollVertically(1)) {
                        mNestedScrollView.smoothScrollBy(0, 1);
                    }
                    try {//must have these busy waits for scrolling to work properly. I assume it breaks because it is currently animating something. Will have to fix this in the future, but it works for now.
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {//must have these waits or else the RecyclerView will have corrupted info
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (mNestedScrollView != null && mNestedScrollView.canScrollVertically(-1)) {
                    if (!sShabbatMode) break;
                    if (mNestedScrollView.canScrollVertically(-1)) {
                        mNestedScrollView.smoothScrollBy(0, -1);
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (sShabbatMode) {
                    startScrollingThread();
                }
            });
            scrollingThread.start();
        }
    }

    /**
     * This method is called when the user wants to end shabbat mode. It will hide the banner and remove the automatic zmanim updater queued task
     * from the handler. I will also reset the color of the calendar button to the default color.
     *
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
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mROZmanimCalendar.getCalendar()));
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
        setupButtons();
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        resetSystemTheme();
        setCustomThemeColors();
        if (binding != null) {
            TextClock clock = binding.clock;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                clock.setVisibility(View.VISIBLE);
                if (Utils.isLocaleHebrew()) {
                    clock.setFormat24Hour("H:mm:ss");
                }
            } else {
                if (sShabbatMode) {
                    clock.setVisibility(View.VISIBLE);
                }
            }
        }
        if (mMainRecyclerView == null) {
            super.onResume();
            return;
        }
        mJewishDateInfo.getJewishCalendar().setInIsrael(sSharedPreferences.getBoolean("inIsrael", false));// check if the user changed the inIsrael setting
        mJewishDateInfo.resetLocale();
        if (sLastTimeUserWasInApp != null) {
            resolveElevationAndVisibleSunrise(() -> {// recreate the zmanim calendar object because it might have changed. Lat, Long, Elevation, or settings might have changed
                instantiateZmanimCalendar();
                mROZmanimCalendar.setCalendar(mCurrentDateShown);// make sure date doesn't change
                setNextUpcomingZman();
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyTextViewTextColor();
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                    mMainRecyclerView.scrollToPosition(mCurrentPosition);
                }
                // update the zmanim notifications if the user changed the settings to start showing them
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
            });
        }

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

        Utils.PrefToWatchSender.send(mContext);

        super.onResume();
    }

    private void setCustomThemeColors() {
        if (mLayout != null && mCalendarButton != null && sSharedPreferences != null && binding != null) {
            if (sSharedPreferences.getBoolean("useImage", false)) {
                Bitmap bitmap = BitmapFactory.decodeFile(sSharedPreferences.getString("imageLocation", ""));
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                mLayout.setBackground(drawable);
            } else if (sSharedPreferences.getBoolean("customBackgroundColor", false) &&
                    !sSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
                int bgColor = sSharedPreferences.getInt("bColor", 0x32312C);
                mLayout.setBackgroundColor(bgColor);
                binding.dailyCard.setCardBackgroundColor(bgColor);
                binding.parshaCard.setCardBackgroundColor(bgColor);
                binding.nextDay.setBackgroundColor(bgColor);
                binding.prevDay.setBackgroundColor(bgColor);
            } else {// use default
                mLayout.setBackgroundColor(0);
                binding.dailyCard.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.cardview_background));
                binding.parshaCard.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.cardview_background));
                binding.nextDay.setBackgroundColor(ContextCompat.getColor(mContext, R.color.buttonColor));
                binding.prevDay.setBackgroundColor(ContextCompat.getColor(mContext, R.color.buttonColor));
            }
            int textColor = sSharedPreferences.getBoolean("customTextColor", false) ? sSharedPreferences.getInt("tColor", 0xFFFFFFFF) : ContextCompat.getColor(mContext, R.color.textColor);
            binding.dailyLocationName.setTextColor(textColor);
            binding.weekday.setTextColor(textColor);
            binding.gregDate.setTextColor(textColor);
            binding.hebDate.setTextColor(textColor);
            binding.parsha.setTextColor(textColor);
            binding.haftara.setTextColor(textColor);
            if (!sShabbatMode) {
                mCalendarButton.setBackgroundColor(sSharedPreferences.getBoolean("useDefaultCalButtonColor", true) ? mContext.getColor(R.color.dark_blue) : sSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
        }
    }

    /**
     * sets the theme of the app according to the user's preferences.
     */
    private void resetSystemTheme() {
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
        sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sSharedPrefListener);
    }

    /**
     * This method accepts a new location object after a request is made from the {@link LocationResolver} class.
     *
     * @param location the input argument
     */
    @Override
    public void accept(Location location) {
        if (sLatitude == 0.0 && sLongitude == 0.0) {// get the last location if it exists
            sLatitude = Double.longBitsToDouble(sSharedPreferences.getLong("Lat", 0));
            sLongitude = Double.longBitsToDouble(sSharedPreferences.getLong("Long", 0));
        }
        if (location != null) {
            sLatitude = location.getLatitude();
            sLongitude = location.getLongitude();
            if (sSharedPreferences != null) {
                sSharedPreferences.edit().putLong("Lat", Double.doubleToRawLongBits(sLatitude)).putLong("Long", Double.doubleToRawLongBits(sLongitude)).apply();
            }
            if (mLocationResolver == null) {
                mLocationResolver = new LocationResolver(mContext, mActivity);
            }
            mLocationResolver.resolveCurrentLocationName();
            mLocationResolver.setTimeZoneID();
            showScrollViewAfterLocationCall();
        } else {// if location object is null, we should check the shimmer visibility to see if it was filled by the other request
            try {
                Thread.sleep(500);// Let's wait a bit to give the program a chance to update the UI
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (binding != null && binding.shimmerLayout.getVisibility() == View.VISIBLE) {
                showScrollViewAfterLocationCall();
                mLocationResolver.acquireLatitudeAndLongitude(this);
            }
        }
    }

    private void showScrollViewAfterLocationCall() {
        synchronized (mJewishDateInfo) {
            mActivity.runOnUiThread(() -> {
                if (mMainRecyclerView != null && mMainRecyclerView.isFocusable()) {
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        if (binding != null) {
                            showWeeklyTextViews();
                        }
                    } else {
                        mMainRecyclerView.setVisibility(View.VISIBLE);
                        if (binding != null) {
                            binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
                        }
                    }
                    resolveElevationAndVisibleSunrise(() -> {
                        if (mCurrentDateShown != null) {
                            instantiateZmanimCalendar();
                            mROZmanimCalendar.setCalendar(mCurrentDateShown);
                            setNextUpcomingZman();
                            createBackgroundThreadForNextUpcomingZman();
                            mJewishDateInfo.setCalendar(mCurrentDateShown);
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyTextViewTextColor();
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                                mMainRecyclerView.scrollToPosition(mCurrentPosition);
                            }
                            if (binding != null) {
                                binding.shimmerLayout.setVisibility(View.GONE);
                            }
                            if (mCalendarButton != null) {
                                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, mCurrentDateShown));
                            }
                            setAllNotifications();
                        }
                    });
                }
            });
        }
    }
}