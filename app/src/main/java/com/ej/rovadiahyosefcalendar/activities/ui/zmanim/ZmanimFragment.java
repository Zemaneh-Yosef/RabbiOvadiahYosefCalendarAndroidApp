package com.ej.rovadiahyosefcalendar.activities.ui.zmanim;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.materialToolbar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentDateShown;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sHebrewDateFormatter;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLastTimeUserWasInApp;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sNavView;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSetupLauncher;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sViewPager;
import static com.ej.rovadiahyosefcalendar.classes.Utils.calculateInSampleSize;
import static com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableDark;
import static com.ej.rovadiahyosefcalendar.classes.Utils.getCurrentCalendarDrawableLight;
import static com.ej.rovadiahyosefcalendar.classes.Utils.inputStreamToString;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.UNSPECIFIED;
import static com.ej.rovadiahyosefcalendar.classes.ZmanimFactory.addZmanim;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
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
import android.location.LocationManager;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity;
import com.ej.rovadiahyosefcalendar.activities.MoladActivity;
import com.ej.rovadiahyosefcalendar.activities.NetzActivity;
import com.ej.rovadiahyosefcalendar.activities.SettingsActivity;
import com.ej.rovadiahyosefcalendar.activities.WelcomeScreenActivity;
import com.ej.rovadiahyosefcalendar.activities.ui.DailySummaryController;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.DailyEntriesBuilder;
import com.ej.rovadiahyosefcalendar.classes.DummyZmanAdapter;
import com.ej.rovadiahyosefcalendar.classes.HebrewDayMonthYearPickerDialog;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.MakamJCal;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZmanimFragment extends Fragment implements Consumer<Location> {

    public static boolean sShabbatMode;
    private int mCurrentPosition;//current position in the list of zmanim to return to
    private boolean mUpdateTablesDialogShown;

    //android classes
    private FragmentZmanimBinding binding;
    private DailySummaryController mDailySummary;// the day summary at the top of the list (hidden on wide screens, where the activity shows it as a sidebar)
    private boolean mIsTwoPane;
    private Context mContext;
    private FragmentActivity mActivity;
    private Handler mHandler = null;
    private Runnable mZmanimUpdater;
    private Runnable mNextZmanUpdater;

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
    public static Instant sNextUpcomingZman = null;
    private Location mNetworkLocation;
    private Location mGpsLocation;
    private Location mCommittedLocation;// the candidate currently reflected in the UI for this request cycle, or null if nothing's been shown yet
    private final Handler mLocationTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable mLocationTimeoutRunnable;
    private static final long LOCATION_UPDATE_TIMEOUT_MS = 5000L;
    private LocationResolver mLocationResolver;
    private ActivityResultLauncher<Intent> mNotificationLauncher;
    private ActivityResultLauncher<String> mBackgroundLocationLauncher;
    private ActivityResultLauncher<String> mPostNotificationsLauncher;
    private SharedPreferences.OnSharedPreferenceChangeListener sSharedPrefListener;
    private static JSONArray makamNames;
    private static final Duration AWAY_THRESHOLD = Duration.ofHours(2);
    private static LocalDate sLastTodayDate;// what "today" was the last time the user was in the app

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = requireActivity();
        initNotifResult();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentZmanimBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        super.onCreate(savedInstanceState);

        mLayout = binding.mainLayout;
        mIsTwoPane = getResources().getBoolean(R.bool.is_two_pane);
        mHandler = new Handler(mContext.getMainLooper());
        initMenu();
        setupShabbatModeBanner();

        mLocationResolver = new LocationResolver(mContext, mActivity);
        if (mLocationName == null) {// if first view is null
            findAllWeeklyViews();
        }

        try {
            makamNames = new JSONArray(inputStreamToString(mActivity.getResources().openRawResource(Utils.isLocaleHebrew(mContext) ? R.raw.makam_names_he : R.raw.makam_names)));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (sLastTimeUserWasInApp == null) {// first time the user sees this page in this process
            // The process may have been started earlier by a notification, the widget or a reboot, which set sCurrentDateShown to the date at
            // that time (possibly yesterday). Always start on today, before anything (like the location callbacks) uses the stale date.
            ZoneId zone = sCurrentTimeZoneID == null ? ZoneId.systemDefault() : ZoneId.of(sCurrentTimeZoneID);
            sCurrentDateShown = LocalDate.now(zone);
            sROZmanimCalendar.setLocalDate(sCurrentDateShown);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
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
                    mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();// even though the location name is updated here, it will be updated later as well and then the UI will be updated
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
        mNotificationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> setNotifications()
        );
        mBackgroundLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        sSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
                    }
                }
        );
        mPostNotificationsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        showNotificationsAreOffDialog();
                    }
                }
        );
    }

    /**
     * This method initializes the main view.
     */
    private void initMainView() {
        if (sLatitude == 0 && sLongitude == 0) {
            requestDeviceLocation();
        }
        mLocationResolver.setTimeZoneID();
        if (binding != null) {
            setupDailyViews();
            // hide everything except the shimmer layout before updating the UI
            setWeeklyViewVisibility(false);
            binding.swipeRefreshLayout.setVisibility(View.GONE);
            binding.shimmerLayout.setVisibility(View.VISIBLE);
        }
        if (sLatitude != 0 && sLongitude != 0) {// the values are updated, the user is using a zipcode or saved location or the app was just restarted
            mLocationResolver.getFullLocationName(true, locationName -> {
                if (locationName == null || locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
                    locationName = sROZmanimCalendar.getGeoLocation().getLocationName();
                }
                sCurrentLocationName = locationName;
                mActivity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.dailySummary.dailyLocationName.setText(sCurrentLocationName);
                    }
                });
                resolveElevationAndVisibleSunrise(() -> {
                    instantiateZmanimCalendar();
                    setNextUpcomingZman();
                    if (binding != null) {
                        updateChaitableTimesFromLatLong(() -> {
                            binding.shimmerLayout.setVisibility(View.GONE);
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                setWeeklyViewVisibility(true);
                                updateWeeklyZmanim();
                            } else {
                                setWeeklyViewVisibility(false);
                                updateDailyZmanim();
                            }
                        });
                    }
                    createBackgroundThreadForNextUpcomingZman();
                });
            });
        }
        setupButtons();
        setNotifications();
        askForRealTimeNotificationPermissions();
        checkIfUserIsInIsraelOrNot();
    }

    /**
     * This method updates the chaitable times for the current location using the Latitude and Longitude. This method should be called before the zmanim
     * list is updated.
     * @param codeToRunAfterwards the code to run after the chaitable times are updated. This should be done on the main UI thread if it the code will affect the UI.
     */
    private void updateChaitableTimesFromLatLong(Runnable codeToRunAfterwards) {//TODO
//        new Thread(() -> {
//            if (sROZmanimCalendar.getHaNetz() == null) {// if visible sunrise for this location is null, try to update it
//                JewishDate jDate = new JewishDate();
//                ChaiTablesWebJava scraper = new ChaiTablesWebJava(new GeoLocation("", sLatitude, sLongitude, TimeZone.getTimeZone(sCurrentTimeZoneID)), jDate);
//
//                try {
//                    ChaiTablesWebJava.ChaiTablesResult[] result = scraper.getChaitableTimesWithLatLong();
//                    int jewishYear = jDate.getJewishYear();
//                    for (ChaiTablesWebJava.ChaiTablesResult r : result) {
//                        ChaiTablesWebJava.saveResultsToFile(r, mContext.getExternalFilesDir(null), sCurrentLocationName, jewishYear);
//                        jewishYear++;
//                    }
//                    sSharedPreferences.edit().putString("chaitablesLink" + sCurrentLocationName, result[0].url()).apply(); // save the link for this location to automatically download again next time
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            mActivity.runOnUiThread(codeToRunAfterwards);
//        }).start();
    }

    private void setupButtons() {
        setupDayHopButtons(true);
        setupCalendarCardAndButton();
        setupDayHopButtons(false);
    }

    /**
     * Sets up the previous day button if true, or the next day button if false.
     */
    private void setupDayHopButtons(boolean previous) {
        if (binding == null) {
            return;
        }

        Button dateBind = (previous ? binding.prevDay : binding.nextDay);
        dateBind.setOnClickListener(v -> {
            if (!sShabbatMode) {
                sCurrentDateShown = sROZmanimCalendar.getLocalDate();//just get a calendar object with the same date as the current one

                int dateChange = (sSharedPreferences.getBoolean("weeklyMode", false) ? 7 : 1) * (previous ? -1 : 1);
                sCurrentDateShown = sCurrentDateShown.plusDays(dateChange);

                sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                sJewishDateInfo.setLocalDate(sCurrentDateShown);
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                seeIfTablesNeedToBeUpdated(true);
            }
        });

        if (previous) {
            mPreviousDate = dateBind;
        } else {
            mNextDate = dateBind;
        }
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
                            .setSelection(sCurrentDateShown.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                            .build();
                    materialDatePicker.addOnPositiveButtonClickListener(selection -> {
                        // Convert the UTC milliseconds selection directly to a LocalDate
                        sCurrentDateShown = Instant.ofEpochMilli(selection)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate();
                        sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                        sJewishDateInfo.setLocalDate(sCurrentDateShown);
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                        }
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                        seeIfTablesNeedToBeUpdated(true);
                    });
                    DatePickerDialog.OnDateSetListener onDateSetListener = (view, year, month, day) -> {
                        LocalDate userChosenDate = LocalDate.of(year, month, day);
                        sROZmanimCalendar.setLocalDate(userChosenDate);
                        sJewishDateInfo.setLocalDate(userChosenDate);
                        sCurrentDateShown = userChosenDate;
                        if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                            updateWeeklyZmanim();
                        } else {
                            updateDailyZmanim();
                        }
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                        seeIfTablesNeedToBeUpdated(true);
                    };
                    materialDatePicker.addOnNegativeButtonClickListener(selection -> {
                        HebrewDayMonthYearPickerDialog hdmypd = new HebrewDayMonthYearPickerDialog(materialDatePicker, mActivity.getSupportFragmentManager(), sJewishDateInfo.getJewishCalendar());
                        hdmypd.updateDate(sJewishDateInfo.getJewishCalendar().getLocalDate());
                        hdmypd.setListener(onDateSetListener);
                        hdmypd.show(mActivity.getSupportFragmentManager(), null);
                    });
                    materialDatePicker.show(mActivity.getSupportFragmentManager(), null);
                }
            });

            if (mDailySummary != null) {
                mDailySummary.setOnChangeDateListener(() -> mCalendarButton.performClick());
            }

            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
        }
    }

    /**
     * This method will automatically update the tables if the user has set up the app before for the current location.
     *
     * @param fromButton if the method is called from the buttons, it will not ask more than once if the user wants to update the tables.
     */
    private void seeIfTablesNeedToBeUpdated(boolean fromButton) {
        if (!(sSharedPreferences.getBoolean("isSetup", false) //only check after the app has been set up before
                && sSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, false))) //and only if the tables are being used
			return;

        if (!ChaiTablesWebJava.checkIfFileDoesNotExist(mActivity.getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.getJewishCalendar().getJewishYear()))
			return;

        if (mUpdateTablesDialogShown)
			return;

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext);
		builder.setTitle(R.string.update_tables);
		builder.setMessage(R.string.the_visible_sunrise_tables_for_the_current_location_and_year_need_to_be_updated_do_you_want_to_update_the_tables_now);
		builder.setPositiveButton(R.string.yes, (dialog, which) -> {
			String chaitablesURL = sSharedPreferences.getString("chaitablesLink" + sCurrentLocationName, "");
			if (!chaitablesURL.isEmpty()) {//it should not be empty if the user has set up the app, but it is good to check
				String hebrewYear = String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear());
				Pattern pattern = Pattern.compile("&cgi_yrheb=\\d{4}");
				Matcher matcher = pattern.matcher(chaitablesURL);
				if (matcher.find()) {
					chaitablesURL = chaitablesURL.replace(matcher.group(), "&cgi_yrheb=" + hebrewYear);//replace the year in the URL with the current year
				}
				ChaiTablesWebJava scraper = new ChaiTablesWebJava(sROZmanimCalendar.getGeoLocation(), sJewishDateInfo.getJewishCalendar());
				String finalChaitablesURL = chaitablesURL;
				Thread thread = new Thread(() -> {
					try {
						ChaiTablesWebJava.ChaiTablesResult[] results = scraper.formatInterfacer(finalChaitablesURL); // will download 2 more years
						int jewishYear = sJewishDateInfo.getJewishCalendar().getJewishYear();
						for (ChaiTablesWebJava.ChaiTablesResult r : results) {
							ChaiTablesWebJava.saveResultsToFile(r, mContext.getExternalFilesDir(null), sCurrentLocationName, jewishYear);
							jewishYear++;
						}
						mActivity.runOnUiThread(() -> {
							if (sSharedPreferences.getBoolean("weeklyMode", false)) {
								updateWeeklyZmanim();
							} else {
								updateDailyZmanim();
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
				thread.start();
			}
		});
		builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
		builder.show();
		if (fromButton) {
			mUpdateTablesDialogShown = true;
		}
    }

    private void setupDailyViews() {
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        swipeRefreshLayout.setOnRefreshListener(() -> new Thread(() -> {
            Looper.prepare();
            if (mLocationResolver == null) {
                mLocationResolver = new LocationResolver(mContext, mActivity);
            }
            requestDeviceLocation();
            mLocationResolver.setTimeZoneID();
            mActivity.runOnUiThread(() -> {
                if (!mActivity.isFinishing()) {
                    checkIfUserIsInIsraelOrNot();
                }
            });
            if (sCurrentDateShown != null && sROZmanimCalendar != null && mMainRecyclerView != null) {
                mActivity.runOnUiThread(() -> {
                    sCurrentDateShown = LocalDate.now();
                    sJewishDateInfo.setLocalDate(sCurrentDateShown);
                });
                mLocationResolver.getFullLocationName(true, locationName -> {
                    if (locationName == null || locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
                        locationName = sROZmanimCalendar.getGeoLocation().getLocationName();
                    }
                    sCurrentLocationName = locationName;
                    mActivity.runOnUiThread(() -> {
                        if (binding != null) {
                            binding.shimmerLayout.setVisibility(View.GONE);
                            binding.dailySummary.dailyLocationName.setText(sCurrentLocationName);
                        }
                    });
                    resolveElevationAndVisibleSunrise(() -> {
                        instantiateZmanimCalendar();
                        setNextUpcomingZman();
                        updateDailyZmanim();
                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                        swipeRefreshLayout.setRefreshing(false);
                    });
                });
            }
            Objects.requireNonNull(Looper.myLooper()).quit();
        }).start());
        mNestedScrollView = binding.nestedScrollView;
        mDailySummary = new DailySummaryController(mContext, binding.dailySummary, false);
        if (mIsTwoPane) {// the day summary is shown in the activity's sidebar instead
            binding.dailySummary.getRoot().setVisibility(View.GONE);
        }
        mMainRecyclerView = binding.mainRV;
        mMainRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mMainRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            mMainRecyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        ViewTreeObserver viewTreeObserver = mMainRecyclerView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnGlobalFocusChangeListener(mFocusFirstZmanOnEntry);// in case the views were set up before
            viewTreeObserver.addOnGlobalFocusChangeListener(mFocusFirstZmanOnEntry);
        }
        RecyclerView dummyRecyclerView = binding.dummyRV;
        dummyRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        dummyRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        dummyRecyclerView.setAdapter(new DummyZmanAdapter(30));
    }

    /**
     * With a D-pad (e.g. on a TV), focus that comes into the zmanim list from outside it (the sidebar, the tabs or the date buttons)
     * starts at the first zman, instead of wherever Android's search for the nearest row happens to land.
     */
    private final ViewTreeObserver.OnGlobalFocusChangeListener mFocusFirstZmanOnEntry = (oldFocus, newFocus) -> {
        RecyclerView list = mMainRecyclerView;
        if (list == null || list.getAdapter() == null || list.getLayoutManager() == null
                || newFocus == null || newFocus.getParent() != list// focus didn't go to a row
                || isInside(oldFocus, list)) {// moving between rows, or the list moving the focus around while it lays itself out
            return;
        }
        for (int position = 0; position < list.getAdapter().getItemCount(); position++) {
            View row = list.getLayoutManager().findViewByPosition(position);
            if (row != null && row.getVisibility() == View.VISIBLE && row.isFocusable()) {// hidden rows (e.g. merged fast times) are skipped
                if (row != newFocus) {
                    row.requestFocus();
                }
                return;
            }
        }
    };

    /** @return true if the view is the given parent or one of its descendants */
    private static boolean isInside(@Nullable View view, View parent) {
        for (View current = view; current != null; current = current.getParent() instanceof View next ? next : null) {
            if (current == parent) {
                return true;
            }
        }
        return false;
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
                            sJewishDateInfo.getJewishCalendar().setInIsrael(true);
                            sSharedPreferences.edit().putBoolean("inIsrael", true).apply();
                            sSharedPreferences.edit().putBoolean("useElevation", true).apply();
                            sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", false).apply();
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            mLocationResolver.getFullLocationName(true, locationName -> {
                                if (locationName == null || locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
                                    locationName = sROZmanimCalendar.getGeoLocation().getLocationName();
                                }
                                sCurrentLocationName = locationName;
                                mActivity.runOnUiThread(() -> {
                                    if (binding != null) {
                                        binding.dailySummary.dailyLocationName.setText(sCurrentLocationName);
                                    }
                                });
                                resolveElevationAndVisibleSunrise(() -> {
                                    instantiateZmanimCalendar();
                                    setNextUpcomingZman();
                                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                        updateWeeklyZmanim();
                                    } else {
                                        updateDailyZmanim();
                                    }
                                    createBackgroundThreadForNextUpcomingZman();
                                });
                            });
                            initMenu();
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
                            sJewishDateInfo.getJewishCalendar().setInIsrael(false);
                            sSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                            sSharedPreferences.edit().putBoolean("useElevation", false).apply();
                            sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", true).apply();
                            Toast.makeText(mContext, R.string.settings_updated, Toast.LENGTH_SHORT).show();
                            sElevation = 0;
                            instantiateZmanimCalendar();
                            setNextUpcomingZman();
                            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                updateWeeklyZmanim();
                            } else {
                                updateDailyZmanim();
                            }
                            createBackgroundThreadForNextUpcomingZman();
                            initMenu();
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

    private void setWeeklyViewVisibility(boolean showWeekly) {
        if (showWeekly) {
            binding.mainWeeklyLayout.getRoot().setVisibility(View.VISIBLE);
            binding.swipeRefreshLayout.setVisibility(View.GONE);

            // 800dp is perfect for the weekly view. However, smaller screens have less space, so we need to shrink it
            float screenHeight = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
            if (screenHeight < 750) {
                binding.mainWeeklyLayout.hebrewDay.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay2.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay3.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay4.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay5.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay6.setTextSize(12);
                binding.mainWeeklyLayout.hebrewDay7.setTextSize(12);

                binding.mainWeeklyLayout.hebrewDate.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate2.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate3.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate4.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate5.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate6.setTextSize(16);
                binding.mainWeeklyLayout.hebrewDate7.setTextSize(16);
            }
        } else {
            binding.mainWeeklyLayout.getRoot().setVisibility(View.GONE);
            binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }

    private void findAllWeeklyViews() {
        mLocationName = binding.mainWeeklyLayout.locationName;
        mEnglishMonthYear = binding.mainWeeklyLayout.englishMonthYear;
        mHebrewMonthYear = binding.mainWeeklyLayout.hebrewMonthYear;
        //there are 7 of these sets of views
        mListViews[0] = binding.mainWeeklyLayout.zmanim1;
        mSunday[1] = binding.mainWeeklyLayout.announcements;
        mSunday[2] = binding.mainWeeklyLayout.hebrewDay;
        mSunday[3] = binding.mainWeeklyLayout.hebrewDate;
        mSunday[4] = binding.mainWeeklyLayout.englishDay;
        mSunday[5] = binding.mainWeeklyLayout.englishDateNumber;

        mListViews[1] = binding.mainWeeklyLayout.zmanim2;
        mMonday[1] = binding.mainWeeklyLayout.announcements2;
        mMonday[2] = binding.mainWeeklyLayout.hebrewDay2;
        mMonday[3] = binding.mainWeeklyLayout.hebrewDate2;
        mMonday[4] = binding.mainWeeklyLayout.englishDay2;
        mMonday[5] = binding.mainWeeklyLayout.englishDateNumber2;

        mListViews[2] = binding.mainWeeklyLayout.zmanim3;
        mTuesday[1] = binding.mainWeeklyLayout.announcements3;
        mTuesday[2] = binding.mainWeeklyLayout.hebrewDay3;
        mTuesday[3] = binding.mainWeeklyLayout.hebrewDate3;
        mTuesday[4] = binding.mainWeeklyLayout.englishDay3;
        mTuesday[5] = binding.mainWeeklyLayout.englishDateNumber3;

        mListViews[3] = binding.mainWeeklyLayout.zmanim4;
        mWednesday[1] = binding.mainWeeklyLayout.announcements4;
        mWednesday[2] = binding.mainWeeklyLayout.hebrewDay4;
        mWednesday[3] = binding.mainWeeklyLayout.hebrewDate4;
        mWednesday[4] = binding.mainWeeklyLayout.englishDay4;
        mWednesday[5] = binding.mainWeeklyLayout.englishDateNumber4;

        mListViews[4] = binding.mainWeeklyLayout.zmanim5;
        mThursday[1] = binding.mainWeeklyLayout.announcements5;
        mThursday[2] = binding.mainWeeklyLayout.hebrewDay5;
        mThursday[3] = binding.mainWeeklyLayout.hebrewDate5;
        mThursday[4] = binding.mainWeeklyLayout.englishDay5;
        mThursday[5] = binding.mainWeeklyLayout.englishDateNumber5;

        mListViews[5] = binding.mainWeeklyLayout.zmanim6;
        mFriday[1] = binding.mainWeeklyLayout.announcements6;
        mFriday[2] = binding.mainWeeklyLayout.hebrewDay6;
        mFriday[3] = binding.mainWeeklyLayout.hebrewDate6;
        mFriday[4] = binding.mainWeeklyLayout.englishDay6;
        mFriday[5] = binding.mainWeeklyLayout.englishDateNumber6;

        mListViews[6] = binding.mainWeeklyLayout.zmanim7;
        mSaturday[1] = binding.mainWeeklyLayout.announcements7;
        mSaturday[2] = binding.mainWeeklyLayout.hebrewDay7;
        mSaturday[3] = binding.mainWeeklyLayout.hebrewDate7;
        mSaturday[4] = binding.mainWeeklyLayout.englishDay7;
        mSaturday[5] = binding.mainWeeklyLayout.englishDateNumber7;

        mWeeklyParsha = binding.mainWeeklyLayout.weeklyParsha;
        mWeeklyHaftorah = binding.mainWeeklyLayout.weeklyHaftorah;
        updateWeeklyTextViewTextColor();
    }

    private void updateWeeklyTextViewTextColor() {
        if (sSharedPreferences.getBoolean("customTextColor", false)) {
            int textColor = sSharedPreferences.getInt("tColor", 0xFFFFFFFF);
            mLocationName.setTextColor(textColor);
            mEnglishMonthYear.setTextColor(textColor);
            mHebrewMonthYear.setTextColor(textColor);
            for (int i = 1; i <= 5; i++) {
                mSunday[i].setTextColor(textColor);
                mMonday[i].setTextColor(textColor);
                mTuesday[i].setTextColor(textColor);
                mWednesday[i].setTextColor(textColor);
                mThursday[i].setTextColor(textColor);
                mFriday[i].setTextColor(textColor);
                mSaturday[i].setTextColor(textColor);
            }
            mWeeklyParsha.setTextColor(textColor);
            mWeeklyHaftorah.setTextColor(textColor);
        }
    }

    private void instantiateZmanimCalendar() {
        sROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                sElevation,
                ZoneId.of(sCurrentTimeZoneID)));
        sROZmanimCalendar.setExternalFilesDir(mActivity.getExternalFilesDir(null));
        String candles = sSettingsPreferences.getString("CandleLightingOffset", "20");
        sROZmanimCalendar.setCandleLightingOffset(Utils.parseDoubleOrDefault(candles, 20));
        String shabbat = sSettingsPreferences.getString("EndOfShabbatOffset", sSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
        sROZmanimCalendar.setAteretTorahSunsetOffset(Utils.parseDoubleOrDefault(shabbat, sSharedPreferences.getBoolean("inIsrael", false) ? 30 : 40));
        if (sSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
            sROZmanimCalendar.setAteretTorahSunsetOffset(30);
        }
        sROZmanimCalendar.setAmudehHoraah(sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false));
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
                        mBackgroundLocationLauncher.launch(ACCESS_BACKGROUND_LOCATION);
                    } else {
                        sSharedPreferences.edit().putBoolean("askedForRealtimeNotifications", true).apply();
                    }
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
            boolean waitingForNotificationPermission = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {// ask for permission to send notifications for newer versions of android ughhhh...
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
                    mPostNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    waitingForNotificationPermission = true;
                } else {
                    if (!sSharedPreferences.getBoolean("hasShownVSNotification", false)) {
                        Utils.showVisibleSunriseNotification(mContext);
                        sSharedPreferences.edit().putBoolean("hasShownVSNotification", true).apply();
                    }
                }
            }

            if (!sSharedPreferences.getBoolean("neverAskBatteryOptimization", false)) {
                PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
                if (!powerManager.isIgnoringBatteryOptimizations(mContext.getPackageName())) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.battery_optimization_is_enabled)
                            .setMessage(R.string.the_current_battery_settings_for_the_app_is_trying_to_save_battery_this_may_cause_notifications_to_be_sent_at_a_later_time_would_you_like_to_change_this_setting)
                            .setCancelable(false)
                            .setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> {
                                try {
                                    mContext.startActivity(new Intent("android.settings.APP_BATTERY_USAGE")
                                            .putExtra("android.intent.extra.PACKAGE_NAME", mContext.getPackageName())
                                            .setData(Uri.parse("package:" + mContext.getPackageName())));
                                } catch (Exception e) {
                                    // Fallback to the main App Info page if the specific battery page fails
                                    Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    fallback.setData(Uri.parse("package:" + mContext.getPackageName()));
                                    Utils.startActivitySafely(mContext, fallback);
                                }
                            })
                            .setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss())
                            .setNeutralButton(mContext.getString(R.string.do_not_ask_me_again), (dialog, which) -> {
                                sSharedPreferences.edit().putBoolean("neverAskBatteryOptimization", true).apply();
                                dialog.dismiss();
                            });
                    if (!mActivity.isFinishing()) {
                        builder.show();
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) mContext.getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {// more annoying android permission garbage
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.zmanim_notifications_will_not_work)
                        .setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications)
                        .setCancelable(false)
                        .setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> {
                            try {
                                mNotificationLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + mContext.getPackageName())));
                            } catch (ActivityNotFoundException e) {// e.g. on some TVs
                                Utils.startActivitySafely(mContext, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + mContext.getPackageName())));
                            }
                        })
                        .setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss());
                if (!mActivity.isFinishing()) {
                    builder.show();
                }
            }

            if (!waitingForNotificationPermission && !NotificationManagerCompat.from(mContext).areNotificationsEnabled()) {
                showNotificationsAreOffDialog();
            }
        }
        setAllNotifications();
    }

    private void showNotificationsAreOffDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.zmanim_notifications_will_not_work)
                .setMessage(R.string.if_you_would_like_to_receive_zmanim_notifications)
                .setPositiveButton(mContext.getString(R.string.yes), (dialog, which) -> Utils.startActivitySafely(mContext, new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, mContext.getPackageName())))
                .setNegativeButton(mContext.getString(R.string.no), (dialog, which) -> dialog.dismiss());
        if (!mActivity.isFinishing()) {
            builder.show();
        }
    }

    private void setAllNotifications() {
        if (sROZmanimCalendar.getGeoLocation().equals(new GeoLocation())) {
            return; // DO NOT SET ANY NOTIFICATIONS WITH A DEFAULT LOCATION
        }
        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Geolocation for notifications = " + sROZmanimCalendar.getGeoLocation().toString() + "\n\n").apply();
        }
        ROZmanimCalendar roZmanimCalendar = sROZmanimCalendar.getCopy();
        roZmanimCalendar.setLocalDate(LocalDate.now()); // do this in order to always set the notifications on the current date
        ROZmanimCalendar tomorrow = roZmanimCalendar.getCopy();
        tomorrow.setLocalDate(tomorrow.getLocalDate().plusDays(1));
        Instant sunrise = roZmanimCalendar.getSunriseBasedOnElevationSetting();
        if (sunrise == null) {
            sunrise = Instant.now();
        }
        if (sunrise.compareTo(Instant.now()) < 0) {
            if (tomorrow.getSunriseBasedOnElevationSetting() != null) {
                sunrise = tomorrow.getSunriseBasedOnElevationSetting();
            } else {
                sunrise = sunrise.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, DailyNotifications.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
        if (BuildConfig.DEBUG) {
            if (sSharedPreferences.getString("debugNotifs", "").length() > 100000) {
                sSharedPreferences.edit().putString("debugNotifs", "").apply();
            }
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Daily Notifications set for: " + sunrise.atZone(roZmanimCalendar.getGeoLocation().getZoneId()) + "\n\n").apply();
        }
        NotificationUtils.setExactAndAllowWhileIdle(am, sunrise.toEpochMilli(), dailyPendingIntent);

        Instant tzeit = roZmanimCalendar.getTzeit();
        if (tzeit == null) {
            tzeit = Instant.now();
        }
        if (tzeit.compareTo(Instant.now()) < 0) {
            if (tomorrow.getTzeit() != null) {
                tzeit = tomorrow.getTzeit();
            } else {
                tzeit = tzeit.plus(1, ChronoUnit.DAYS);
            }
        }
        PendingIntent omerPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, OmerNotifications.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        if (BuildConfig.DEBUG) {
            sSharedPreferences.edit().putString("debugNotifs", sSharedPreferences.getString("debugNotifs", "") + "Omer Notifications set for: " + tzeit.atZone(roZmanimCalendar.getGeoLocation().getZoneId()) + "\n\n").apply();
        }
        NotificationUtils.setExactAndAllowWhileIdle(am, tzeit.toEpochMilli(), omerPendingIntent);

        Intent zmanIntent = new Intent(mContext, ZmanimNotifications.class);
        PendingIntent zmanimPendingIntent = PendingIntent.getBroadcast(mContext, 0, zmanIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
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
            if (mNestedScrollView != null) {
                mNestedScrollView.scrollTo(0, 0);
            }
            MainFragmentManagerActivity.refreshSidebar();
        }
    }

    /**
     * Updates the day summary at the top of the list and returns the entries for the zmanim list. On wide screens the summary
     * and the info rows are shown in the activity's sidebar instead, so only the zmanim and seasonal hours are returned.
     *
     * @return List of ZmanListEntry objects that will be used to populate the list view.
     * @see ZmanListEntry
     */
    private List<ZmanListEntry> getZmanimList(boolean add66MisheyakirZman) {
        List<ZmanListEntry> zmanim = DailyEntriesBuilder.build(mContext, add66MisheyakirZman);
        if (mIsTwoPane) {
            return DailyEntriesBuilder.timesOnly(zmanim);
        }
        if (mDailySummary != null) {
            mDailySummary.refresh(null);
        }
        return zmanim;
    }

    private void initMenu() {
        if (materialToolbar == null) {
            return;
        }
        if (Utils.isLocaleHebrew(mContext)) {
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
                if (!sShabbatMode && sROZmanimCalendar != null && mMainRecyclerView != null) {
                    sCurrentDateShown = LocalDate.now();
                    sJewishDateInfo.setLocalDate(sCurrentDateShown);
                    sROZmanimCalendar.setLocalDate(sCurrentDateShown);
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
                setWeeklyViewVisibility(item.isChecked());
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
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
                Utils.startActivitySafely(mContext, browserIntent);
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

    private void createBackgroundThreadForNextUpcomingZman() {
        if (mNextZmanUpdater != null) {
            mHandler.removeCallbacks(mNextZmanUpdater);
        }
        mNextZmanUpdater = () -> {
            setNextUpcomingZman();
            if (mNestedScrollView != null && !sSharedPreferences.getBoolean("weeklyMode", false)) {
                mCurrentPosition = mNestedScrollView.getScrollY();
                updateDailyZmanim();
                mNestedScrollView.scrollTo(0, mCurrentPosition);
            } else if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            }
            createBackgroundThreadForNextUpcomingZman();//start a new thread to update the next upcoming zman
        };
        if (sNextUpcomingZman != null) {
            mHandler.postDelayed(mNextZmanUpdater, sNextUpcomingZman.toEpochMilli() - Instant.now().toEpochMilli() + 1_000);//add 1 second to make sure we don't get the same zman again
        }
    }

    public void setNextUpcomingZman() {
        ZmanListEntry nextZman = ZmanimFactory.getNextUpcomingZman(sCurrentDateShown, sROZmanimCalendar, sJewishDateInfo, sSettingsPreferences, sSharedPreferences);
        if (nextZman == null || nextZman.getZman() == null) {
            nextZman = new ZmanListEntry("", Instant.now().plusSeconds(30), SecondTreatment.ROUND_EARLIER, UNSPECIFIED,"");// try again in 30 seconds
        }
        sNextUpcomingZman = nextZman.getZman();
    }

    private String getAnnouncements() {
        StringBuilder announcements = new StringBuilder();

        String day = sJewishDateInfo.getSpecialDay(true);
        if (!day.isEmpty()) {
            announcements.append(day.replace("/ ", "\n")).append("\n");
        }

        if (sJewishDateInfo.isEruvTavshilimMadeToday()) {
            announcements.append(mContext.getString(R.string.eruv_tavshilin)).append("\n");
        }

        if (sJewishDateInfo.isPurimMeshulash()) {
            announcements.append(mContext.getString(R.string.no_tachanun_in_yerushalayim)).append("\n");
        }

        if (sSettingsPreferences.getBoolean("showShabbatMevarchim", true)) {
            if (sJewishDateInfo.getJewishCalendar().isShabbosMevorchim()) {
                announcements.append("שבת מברכים").append("\n");
            }
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
        if (!tachanun.equals(mContext.getString(R.string.there_is_tachanun_today))) {
            announcements.append(tachanun).append("\n");
        }

        String birchatLevana = sJewishDateInfo.getBirchatLevana(ZoneId.of(sCurrentTimeZoneID));
        if (!birchatLevana.isEmpty() && !birchatLevana.contains("until") && !birchatLevana.contains("עד")) {
            announcements.append(birchatLevana).append("\n");
        }

        if (sJewishDateInfo.getJewishCalendar().isBirkasHachamah()) {
            announcements.append(mContext.getString(R.string.birchat_hachamah_is_said_today)).append("\n");
        }

        if (sJewishDateInfo.tomorrow().getJewishCalendar().getDayOfWeek() == Calendar.SATURDAY
                && sJewishDateInfo.tomorrow().getJewishCalendar().getYomTovIndex() == JewishCalendar.EREV_PESACH) {
            announcements.append(mContext.getString(R.string.burn_your_ametz_today)).append("\n");
        }

        String tekufaOpinions = sSettingsPreferences.getString("TekufaOpinions", "1");
        List<ZmanListEntry> tekufa = new ArrayList<>();
        switch (tekufaOpinions) {
            case "1":
                DailyEntriesBuilder.addTekufaTime(mContext, tekufa, true, sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false));
                break;
            case "2":
                DailyEntriesBuilder.addTekufaTime(mContext, tekufa, true, false);
                break;
            case "3":
                DailyEntriesBuilder.addTekufaTime(mContext, tekufa, true, true);
                break;
            default:// 4
                DailyEntriesBuilder.addTekufaTime(mContext, tekufa, true, true);
                DailyEntriesBuilder.addTekufaTime(mContext, tekufa, true, false);
                break;
        }
        if (!tekufa.isEmpty()) {
            for (ZmanListEntry tekufaEntry : tekufa) {
                announcements.append(tekufaEntry.getTitle()).append("\n");
            }
        }

        try {
            Map<String, List<MakamJCal.Makam>> makamData = MakamJCal.Companion.getMakamData(sJewishDateInfo.getJewishCalendar());
            if (makamData.containsKey("GABRIEL A SHREM 1964 SUHV")) {
                List<MakamJCal.Makam> makamObj = makamData.get("GABRIEL A SHREM 1964 SUHV");
                StringBuilder makamText = new StringBuilder(mContext.getString(R.string.makam));
                if (makamObj != null) {
                    for (int i = 0; i < makamObj.size(); i++) {
                        makamText.append(makamNames.get(makamObj.get(i).ordinal())).append(" ");
                    }
                }

                announcements.append(makamText).append("\n");
            } else if (makamData.containsKey("ADES: 24793")) {
                List<MakamJCal.Makam> makamObj = makamData.get("ADES: 24793");
                StringBuilder makamText = new StringBuilder(mContext.getString(R.string.makam));
                if (makamObj != null) {
                    for (int i = 0; i < makamObj.size(); i++) {
                        makamText.append(makamNames.get(makamObj.get(i).ordinal())).append(" ");
                    }
                }

                announcements.append(makamText.toString().trim()).append("\n");
            } else if (makamData.containsKey("Eliahou Yaaqob DWECK-KESAR")) {
                List<MakamJCal.Makam> makamObj = makamData.get("Eliahou Yaaqob DWECK-KESAR");
                StringBuilder makamText = new StringBuilder(mContext.getString(R.string.makam));
                if (makamObj != null) {
                    for (int i = 0; i < makamObj.size(); i++) {
                        makamText.append(makamNames.get(makamObj.get(i).ordinal())).append(" ");
                    }
                }

                announcements.append(makamText.toString().trim()).append("\n");
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        if (announcements.length() > 0 && announcements.charAt(announcements.length() - 1) == '\n') {
            announcements.deleteCharAt(announcements.length() - 1);
        }
        return announcements.toString();
    }

    private void updateWeeklyZmanim() {
        LocalDate backup = sCurrentDateShown;
        LocalDate sunday = sCurrentDateShown.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        sROZmanimCalendar.setLocalDate(sunday);//set the calendar to the sunday of that week
        sJewishDateInfo.setLocalDate(sunday);

        HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
        if (Utils.isLocaleHebrew(mContext)) {
            hebrewDateFormatter.setHebrewFormat(true);
        }
        List<TextView[]> weeklyInfo = Arrays.asList(mSunday, mMonday, mTuesday, mWednesday, mThursday, mFriday, mSaturday);

        String month = sunday.getMonth().getDisplayName(TextStyle.FULL, mContext
                .getResources()
                .getConfiguration()
                .getLocales()
                .get(0));
        String year = String.valueOf(sunday.getYear());

        String hebrewMonth = hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet");
        String hebrewYear = String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear());
        if (Utils.isLocaleHebrew(mContext)) {
            hebrewYear = hebrewDateFormatter.formatHebrewNumber(sJewishDateInfo.getJewishCalendar().getJewishYear());
        }

        for (int i = 0; i < 7; i++) {
            if (sROZmanimCalendar.getLocalDate().equals(LocalDate.now())) {
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
            });//E.G. "Candle Lighting: 5:45 AM, Rabbenu Tam: 8:30 PM, etc."
            if (!mZmanimForAnnouncements.isEmpty()) {
                for (String zman : mZmanimForAnnouncements) {
                    announcements.append(zman).append("\n");
                }
            }
            announcements.append(getAnnouncements());
            if (announcements.length() > 0 && announcements.charAt(announcements.length() - 1) == '\n') {
                announcements.deleteCharAt(announcements.length() - 1);
            }
            weeklyInfo.get(i)[1].setText(announcements.toString());//E.G. "Succot, Yom Kippur, etc."
            if (announcements.toString().isEmpty()) {
                weeklyInfo.get(i)[1].setVisibility(View.INVISIBLE);
            } else {
                weeklyInfo.get(i)[1].setVisibility(View.VISIBLE);
            }
            weeklyInfo.get(i)[2].setText(sJewishDateInfo.getJewishDayOfWeek());//E.G. "יום ראשון"
            weeklyInfo.get(i)[3].setText(sHebrewDateFormatter.formatHebrewNumber(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth()));//E.G. "א"
            weeklyInfo.get(i)[4].setText(sROZmanimCalendar.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, mContext
                    .getResources()
                    .getConfiguration()
                    .getLocales()
                    .get(0)));//E.G. "Sun"
            weeklyInfo.get(i)[5].setText(String.valueOf(sROZmanimCalendar.getLocalDate().getDayOfMonth()));//E.G. "6"
            if (i != 6) {
                sROZmanimCalendar.setLocalDate(sROZmanimCalendar.getLocalDate().plusDays(1));
                sJewishDateInfo.setLocalDate(sROZmanimCalendar.getLocalDate());
            }
        }
        if (month != null && !month.equals(sROZmanimCalendar.getLocalDate().getMonth().getDisplayName(TextStyle.FULL, mContext
                .getResources()
                .getConfiguration()
                .getLocales()
                .get(0)))) {
            month += " - " + sROZmanimCalendar.getLocalDate().getMonth().getDisplayName(TextStyle.FULL, mContext
                    .getResources()
                    .getConfiguration()
                    .getLocales()
                    .get(0));
        }
        if (!year.equals(String.valueOf(sROZmanimCalendar.getLocalDate().getYear()))) {
            year += " / " + sROZmanimCalendar.getLocalDate().getYear();
        }
        if (!hebrewMonth.equals(hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                .replace("Tishrei", "Tishri")
                .replace("Teves", "Tevet"))) {
            hebrewMonth += " - " + hebrewDateFormatter.formatMonth(sJewishDateInfo.getJewishCalendar())
                    .replace("Tishrei", "Tishri")
                    .replace("Teves", "Tevet");
        }
        if (Utils.isLocaleHebrew(mContext)) {
            if (!hebrewYear.equals(hebrewDateFormatter.formatHebrewNumber(sJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + hebrewDateFormatter.formatHebrewNumber(sJewishDateInfo.getJewishCalendar().getJewishYear());
            }
        } else {
            if (!hebrewYear.equals(String.valueOf(sJewishDateInfo.getJewishCalendar().getJewishYear()))) {
                hebrewYear += " / " + sJewishDateInfo.getJewishCalendar().getJewishYear();
            }
        }
        String monthYear = month + " " + year;
        mEnglishMonthYear.setText(monthYear);
        if (Utils.isLocaleHebrew(mContext)) {
            mEnglishMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            mHebrewMonthYear.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
        mLocationResolver.getFullLocationName(true, locationName -> {
            if (locationName != null) {
                mLocationName.setText(locationName);
            }
        });
        String hebrewMonthYear = hebrewMonth + " " + hebrewYear;
        mHebrewMonthYear.setText(hebrewMonthYear);
        mWeeklyParsha.setText(sJewishDateInfo.getThisWeeksParsha());
        mWeeklyParsha.setVisibility(View.VISIBLE);
        if (sJewishDateInfo.getThisWeeksHaftarah().length() == 0) {
            mWeeklyHaftorah.setVisibility(View.GONE);
        } else {
            mWeeklyHaftorah.setVisibility(View.VISIBLE);
            mWeeklyHaftorah.setText(sJewishDateInfo.getThisWeeksHaftarah());
        }
        sROZmanimCalendar.setLocalDate(backup);
        sJewishDateInfo.setLocalDate(backup);
        MainFragmentManagerActivity.refreshSidebar();
    }

    private String[] getShortZmanim() {
        List<ZmanListEntry> zmanim = new ArrayList<>();
        addZmanim(zmanim, true, sSettingsPreferences, sSharedPreferences, sROZmanimCalendar, sJewishDateInfo, true);

        //filter out important zmanim
        List<ZmanListEntry> zmansToRemove = new ArrayList<>();
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    String zmanTime = zman.getZman() == null ? "XX:XX" : Utils.formatZmanTime(mContext, zman);
                    if (!Utils.isLocaleHebrew(mContext)) {
                        mZmanimForAnnouncements.add(zmanTime + " :" + zman.getTitle().replaceAll("\\(.*\\)", "").trim());
                    } else {
                        mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanTime);
                    }
                    zmansToRemove.add(zman);
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                if (zman.isNoteworthyZman()) {
                    String zmanTime = zman.getZman() == null ? "XX:XX" : Utils.formatZmanTime(mContext, zman);
                    mZmanimForAnnouncements.add(zman.getTitle().replaceAll("\\(.*\\)", "").trim() + ": " + zmanTime);
                    zmansToRemove.add(zman);
                }
            }
        }
        zmanim.removeAll(zmansToRemove);

        //here is where we actually create the list of zmanim to display
        String[] shortZmanim = new String[zmanim.size()];
        if (sSharedPreferences.getBoolean("isZmanimInHebrew", false)) {
            for (ZmanListEntry zman : zmanim) {
                String zmanTime = zman.getZman() == null ? "XX:XX" : Utils.formatZmanTime(mContext, zman);
                if (!Utils.isLocaleHebrew(mContext)) {
                    shortZmanim[zmanim.indexOf(zman)] = zmanTime + " :" + zman.getTitle()
                            .replace("סוף זמן ", "")
                            .replace("(", "")
                            .replace(")", "");
                } else {
                    shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                            .replace("סוף זמן ", "")
                            .replace("(", "")
                            .replace(")", "") + ": " + zmanTime;
                }

                if (zman.getZman() != null && zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] +
                            (Utils.isLocaleHebrew(mContext) ? " ➤ " : " ◄ ");
                }
            }
        } else {
            for (ZmanListEntry zman : zmanim) {
                String zmanTime = zman.getZman() == null ? "XX:XX" : Utils.formatZmanTime(mContext, zman);
                shortZmanim[zmanim.indexOf(zman)] = zman.getTitle()
                        .replace("Earliest ", "")
                        .replace("Sof Zeman ", "")
                        .replace("Latest ", "")
                        .replace("(", "")
                        .replace(")", "")
                        + ": " + zmanTime;
                if (zman.getZman() != null && zman.getZman().equals(sNextUpcomingZman)) {
                    shortZmanim[zmanim.indexOf(zman)] = shortZmanim[zmanim.indexOf(zman)] +
                            (Utils.isLocaleHebrew(mContext) ? " ➤ " : " ◄ ");
                }
            }
        }
        return shortZmanim;
    }

    /**
     * This method will check if elevation has been set for the current location and if not, it will automatically set it by connecting to geonames
     * and getting the elevation for the current location via the LocationResolver class. It will then set the elevation in the shared preferences
     * for the current location. If the user is offline, it will not do anything. The only time it will not set the elevation is if the user has
     * disabled the elevation setting in the settings menu. This method should be called after the location name has been set.
     */
    private void resolveElevationAndVisibleSunrise(Runnable codeToRunOnMainThread) {
        boolean sUserIsOffline = false;
        if (sCurrentLocationName != null && !sCurrentLocationName.isEmpty()) {// Somehow it is null and crashing in some places
            if (sCurrentLocationName.contains("Lat:") && sCurrentLocationName.contains("Long:")
                    && sSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false)) {//only if the user has enabled the setting to set the elevation to the last known location
                sUserIsOffline = true;
                sElevation = Utils.parseDoubleOrDefault(sSharedPreferences.getString("elevation" + sSharedPreferences.getString("name", ""), "0"), 0);//lastKnownLocation
            } else {//user is online, get the elevation from the shared preferences for the current location
                sElevation = Utils.parseDoubleOrDefault(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"), 0);//get the last value of the current location or 0 if it doesn't exist
            }
        }

        if (!sUserIsOffline && sSharedPreferences.getBoolean("useElevation", true)) {//update if the user is online and the elevation setting is enabled
            if (!sSharedPreferences.contains("elevation" + sCurrentLocationName)) {//if the elevation for this location has never been set
                Thread thread = new Thread(() -> mLocationResolver.getElevationFromWebService(mHandler,
                        () -> sElevation = Utils.parseDoubleOrDefault(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"), 0),
                        codeToRunOnMainThread));
                thread.start();
                seeIfTablesNeedToBeUpdated(false);
            } else {// use elevation that was set before
                sElevation = Utils.parseDoubleOrDefault(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"), 0);
                mActivity.runOnUiThread(codeToRunOnMainThread);
            }
        } else {// user does not want elevation
            sElevation = 0;
            mActivity.runOnUiThread(codeToRunOnMainThread);
        }
    }

    private final Runnable mClockTicker = new Runnable() {
        @Override
        public void run() {
            if (binding == null) return;
            String pattern = android.text.format.DateFormat.is24HourFormat(mContext) ? "H:mm:ss" : "h:mm:ss a";
            long now = System.currentTimeMillis();
            binding.clock.setText(android.text.format.DateFormat.format(pattern, now));
            mHandler.postDelayed(this, 1000 - now % 1000);// next tick on the second boundary
        }
    };

    private void showClock(boolean show) {
        if (binding == null) return;
        mHandler.removeCallbacks(mClockTicker);
        binding.clock.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) mClockTicker.run();
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
     * and update the date when the time reaches the next date at 12:00:01am. It will also update the shabbat banner to reflect the next day's date.
     * (The reason why I chose 12:00:01am is to avoid a hiccup if the device is too fast to update the time, although it is probably not a big deal.)
     *
     * @see #startScrollingThread() to start the thread that will scroll through the list of zmanim
     * @see #setShabbatBannerColors(boolean) to set the text of the shabbat banners
     */
    private void startShabbatMode() {
        if (!sShabbatMode) {
            sShabbatMode = true;
            setShabbatBannerColors(true);
            mShabbatModeBanner.setVisibility(View.VISIBLE);
            if (sNavView != null && sViewPager != null) {
                sNavView.setVisibility(View.GONE);
                sViewPager.setUserInputEnabled(false);
            }
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mActivity.getWindow().setHideOverlayWindows(true);
            }
            showClock(true);// always show the clock in shabbat mode
            mNextDate.setVisibility(View.GONE);
            mPreviousDate.setVisibility(View.GONE);

            final ZoneId zone = sCurrentTimeZoneID == null ? ZoneId.systemDefault() : ZoneId.of(sCurrentTimeZoneID);
            mZmanimUpdater = () -> {
                LocalDate today = LocalDate.now(zone);
                sCurrentDateShown = today;
                sROZmanimCalendar.setLocalDate(today);
                sJewishDateInfo.setLocalDate(today);
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                setShabbatBannerColors(false);
                if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                    updateWeeklyZmanim();
                } else {
                    updateDailyZmanim();
                }
                mHandler.removeCallbacks(mZmanimUpdater);
                mHandler.postDelayed(mZmanimUpdater, millisUntilNextDayStart(zone));// run again at the next 12:00:01am
            };
            mHandler.postDelayed(mZmanimUpdater, millisUntilNextDayStart(zone));
            startScrollingThread();
        }
    }

    /**
     * Milliseconds from now until 12:00:01am tomorrow in the given zone. The 1 second avoids firing right on the boundary.
     */
    private static long millisUntilNextDayStart(ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.toLocalDate().plusDays(1).atStartOfDay(zone).plusSeconds(1);
        return Duration.between(now, next).toMillis();
    }

    /**
     * Sets the text of the shabbat banners based on the NEXT day's date, since most people will start shabbat mode before shabbat/chag starts.
     *
     * @param isFirstTime if true, the text will be set based on the next day's date, otherwise it will be set based on the current date.
     *                    Since it will be called at 12:00:02am the next day, we do not need to worry about the next day's date.
     */
    private void setShabbatBannerColors(boolean isFirstTime) {
        if (isFirstTime) {
            sCurrentDateShown = sCurrentDateShown.plusDays(1);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
        }

        boolean isShabbat = sCurrentDateShown.getDayOfWeek() == DayOfWeek.SATURDAY;

        StringBuilder sb = new StringBuilder();

        switch (sJewishDateInfo.getJewishCalendar().getYomTovIndex()) {
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableDark(sCurrentDateShown));
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
                mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, getCurrentCalendarDrawableLight(sCurrentDateShown));
        }

        if (isFirstTime) {
            sCurrentDateShown = sCurrentDateShown.minusDays(1);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
        }
    }

    /**
     * This method is called when the user clicks on shabbat mode. It schedules a self-rescheduling task on
     * the main thread's Handler that scrolls the list down to the bottom, pauses, scrolls back up to the top,
     * pauses, and repeats for as long as Shabbat mode is active.
     */
    private void startScrollingThread() {
        if (!sSharedPreferences.getBoolean("weeklyMode", false)) {
            scrollDownStep();
        }
    }

    private void scrollDownStep() {
        if (!sShabbatMode || mNestedScrollView == null || !mNestedScrollView.canScrollVertically(1)) {
            mHandler.postDelayed(this::scrollUpStep, 1000);// pause, then start the scroll-up phase
            return;
        }
        mNestedScrollView.smoothScrollBy(0, 1);
        mHandler.postDelayed(this::scrollDownStep, 10);
    }

    private void scrollUpStep() {
        if (!sShabbatMode || mNestedScrollView == null || !mNestedScrollView.canScrollVertically(-1)) {
            if (sShabbatMode) {
                mHandler.postDelayed(this::startScrollingThread, 1000);// pause, then restart the whole cycle
            }
            return;
        }
        mNestedScrollView.smoothScrollBy(0, -1);
        mHandler.postDelayed(this::scrollUpStep, 10);
    }

    /**
     * This method is called when the user wants to end shabbat mode. It will hide the banner and remove the automatic zmanim updater queued task
     * from the handler. I will also reset the color of the calendar button to the default color.
     *
     * @see #startScrollingThread()
     * @see #startShabbatMode()
     */
    private void endShabbatMode() {
        if (!sShabbatMode) return;

        sShabbatMode = false;
        mShabbatModeBanner.setVisibility(View.GONE);
        if (sNavView != null && sViewPager != null && sSettingsPreferences != null && !sSettingsPreferences.getBoolean("hideBottomBar", false)) {
            sNavView.setVisibility(View.VISIBLE);
            sViewPager.setUserInputEnabled(true);
        }
        mHandler.removeCallbacks(mZmanimUpdater);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mActivity.getWindow().setHideOverlayWindows(false);
        }
        if (sSharedPreferences.getBoolean("useDefaultCalButtonColor", true)) {
            mCalendarButton.setBackgroundColor(mContext.getColor(R.color.dark_blue));
        } else {
            mCalendarButton.setBackgroundColor(sSharedPreferences.getInt("CalButtonColor", 0x18267C));
        }
        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sROZmanimCalendar.getLocalDate()));
        mNextDate.setVisibility(View.VISIBLE);
        mPreviousDate.setVisibility(View.VISIBLE);
        showClock(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onPause() {
        sLastTimeUserWasInApp = Instant.now();
        if (mNestedScrollView != null) {
            mCurrentPosition = mNestedScrollView.getScrollY();
        }
        mHandler.removeCallbacks(mClockTicker);
        super.onPause();
    }

    @Override
    public void onResume() {
        initMenu();
        setupButtons();
        if (sSettingsPreferences.getBoolean("mainAlwaysOn", false) || sShabbatMode) {
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        resetSystemTheme();
        setCustomThemeColors();
        showClock(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || sShabbatMode);
        if (mMainRecyclerView == null) {
            super.onResume();
            return;
        }
        sJewishDateInfo.getJewishCalendar().setInIsrael(sSharedPreferences.getBoolean("inIsrael", false));// check if the user changed the inIsrael setting
        sJewishDateInfo.resetLocale(mContext);
        try {
            makamNames = new JSONArray(inputStreamToString(mActivity.getResources().openRawResource(Utils.isLocaleHebrew(mContext) ? R.raw.makam_names_he : R.raw.makam_names)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (mDailySummary != null) {
            mDailySummary.reloadMakamNames();
        }
        if (sLastTimeUserWasInApp != null) {
            mLocationResolver.getFullLocationName(true, locationName -> {
                if (locationName == null || locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
                    locationName = sROZmanimCalendar.getGeoLocation().getLocationName();
                }
                sCurrentLocationName = locationName;
                mActivity.runOnUiThread(() -> {
                    if (binding != null) {
                        binding.dailySummary.dailyLocationName.setText(sCurrentLocationName);
                    }
                });
                resolveElevationAndVisibleSunrise(() -> {// recreate the zmanim calendar object because it might have changed. Lat, Long, Elevation, or settings might have changed
                    instantiateZmanimCalendar();
                    sROZmanimCalendar.setLocalDate(sCurrentDateShown);// make sure date doesn't change
                    setNextUpcomingZman();
                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                        updateWeeklyTextViewTextColor();
                        updateWeeklyZmanim();
                    } else {
                        updateDailyZmanim();
                        if (mNestedScrollView != null) {
                            mNestedScrollView.scrollTo(0, mCurrentPosition);
                        }
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
            });
        }

        ZoneId zone = sCurrentTimeZoneID == null ? ZoneId.systemDefault() : ZoneId.of(sCurrentTimeZoneID);
        LocalDate today = LocalDate.now(zone);
        Instant now = Instant.now();
        if (sLastTimeUserWasInApp == null) {
            sLastTimeUserWasInApp = now;
        }
        if (sLastTodayDate == null) {
            sLastTodayDate = today;
        }

        boolean awayTooLong = Duration.between(sLastTimeUserWasInApp, now).compareTo(AWAY_THRESHOLD) > 0;
        // a new day only moves the user along if they were looking at "today"; someone browsing another date keeps it
        boolean followToday = !today.equals(sLastTodayDate) && sLastTodayDate.equals(sCurrentDateShown);

        if (awayTooLong || followToday || sCurrentDateShown == null) {
            sCurrentDateShown = today;
            sROZmanimCalendar.setLocalDate(sCurrentDateShown);
            sJewishDateInfo.setLocalDate(sCurrentDateShown);
            if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                updateWeeklyZmanim();
            } else {
                updateDailyZmanim();
                if (mNestedScrollView != null) {
                    mNestedScrollView.scrollTo(0, mCurrentPosition);
                }
            }
        }
        sLastTodayDate = today;

        Utils.PrefToWatchSender.send(mContext);

        super.onResume();
    }

    private void setCustomThemeColors() {
        if (mLayout != null && mCalendarButton != null && sSharedPreferences != null && binding != null) {
            if (sSharedPreferences.getBoolean("useImage", false)) {
                String imagePath = sSharedPreferences.getString("imageLocation", "");
                if (!imagePath.isEmpty()) {
                    // Get screen dimensions to use as target width/height
                    android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                    int reqWidth = displayMetrics.widthPixels;
                    int reqHeight = displayMetrics.heightPixels;

                    // First decode with inJustDecodeBounds=true to check dimensions without loading into memory
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(imagePath, options);
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                    options.inJustDecodeBounds = false;// Decode bitmap with inSampleSize set
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                    if (bitmap != null) {
                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        mLayout.setBackground(drawable);
                    }
                }
            } else if (sSharedPreferences.getBoolean("customBackgroundColor", false) &&
                    !sSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
                int bgColor = sSharedPreferences.getInt("bColor", 0x32312C);
                mLayout.setBackgroundColor(bgColor);
                binding.nextDay.setBackgroundColor(bgColor);
                binding.prevDay.setBackgroundColor(bgColor);
            } else {// use default
                mLayout.setBackgroundColor(0);
                binding.nextDay.setBackgroundColor(ContextCompat.getColor(mContext, R.color.buttonColor));
                binding.prevDay.setBackgroundColor(ContextCompat.getColor(mContext, R.color.buttonColor));
            }
            if (mDailySummary != null) {
                mDailySummary.applyThemeColors();
            }
            if (!sShabbatMode) {
                mCalendarButton.setBackgroundColor(sSharedPreferences.getBoolean("useDefaultCalButtonColor", true) ? mContext.getColor(R.color.dark_blue) : sSharedPreferences.getInt("CalButtonColor", 0x18267C));
            }
            mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
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
        if (binding != null && binding.getRoot().getViewTreeObserver().isAlive()) {
            binding.getRoot().getViewTreeObserver().removeOnGlobalFocusChangeListener(mFocusFirstZmanOnEntry);
        }
        binding = null;
        mDailySummary = null;
        sSharedPreferences.unregisterOnSharedPreferenceChangeListener(sSharedPrefListener);
        if (mLocationTimeoutRunnable != null) {
            mLocationTimeoutHandler.removeCallbacks(mLocationTimeoutRunnable);
        }
        mHandler.removeCallbacksAndMessages(null);
    }


    /**
     * Starts a fresh device-location request. Resets any location candidates left over from a
     * previous call, and arms a 5-second hard timeout so the UI is never blocked indefinitely
     * waiting for the (usually slower, more accurate) GPS result.
     */
    private void requestDeviceLocation() {
        mNetworkLocation = null;
        mGpsLocation = null;
        mCommittedLocation = null;

        if (mLocationTimeoutRunnable != null) {
            mLocationTimeoutHandler.removeCallbacks(mLocationTimeoutRunnable);
        }
        mLocationTimeoutRunnable = this::onLocationTimeout;
        mLocationTimeoutHandler.postDelayed(mLocationTimeoutRunnable, LOCATION_UPDATE_TIMEOUT_MS);

        mLocationResolver.acquireLatitudeAndLongitude(this);
    }

    /**
     * Fires if neither location candidate has resolved within {@link #LOCATION_UPDATE_TIMEOUT_MS}.
     * Falls back to whatever's available (a partial result, or the last-known lat/long already in
     * shared preferences) rather than leaving the UI stuck on the shimmer indefinitely.
     */
    private void onLocationTimeout() {
        if (mCommittedLocation != null) {
            return;// already showing something for this cycle; nothing to force
        }
        commitLocation(pickBestLocation());// almost certainly null here - finalizeLocation falls back to cached lat/long
    }

    /**
     * This method accepts a new location object after a request is made from the {@link LocationResolver} class.
     * Both the NETWORK and GPS requests report here. We hold onto both and only act once we can
     * determine the more accurate one (via {@link Location#getElapsedRealtimeNanos()}), a 5-second
     * timeout elapses, or both providers have reported in (whichever comes first).
     * @param location the input argument
     */
    @Override
    public void accept(Location location) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mHandler.post(() -> accept(location));
            return;
        }

        if (location != null) {
            if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
                mGpsLocation = location;
            } else {
                mNetworkLocation = location;
            }
        }

        if (mCommittedLocation == null) {
            if (location != null) {
                // Nothing shown yet this cycle - show the first result the instant it arrives.
                commitLocation(pickBestLocation());
            } else if (!locationManagerHasBothProviders()) {
                // Only one provider exists on this device, and it just reported null - nothing else is coming.
                commitLocation(null);
            }
            // else: this was a null from one of two providers; the other might still succeed - wait for it, or the 5s timeout.
            return;
        }

        // Already showing a result. Only re-render if the newly completed picture is actually better.
        Location best = pickBestLocation();
        if (best != null && best != mCommittedLocation) {
            commitLocation(best);
        }
    }

    private boolean locationManagerHasBothProviders() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null
                && locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)
                && locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER);
    }

    /**
     * Picks the more accurate of the two held candidates using elapsed-realtime-based freshness
     * as a tiebreaker/quality signal, per Android's own recommendation for comparing Location objects.
     */
    private Location pickBestLocation() {
        if (mGpsLocation == null) return mNetworkLocation;
        if (mNetworkLocation == null) return mGpsLocation;

        // Prefer explicit accuracy (lower = better) when both report it.
        if (mGpsLocation.hasAccuracy() && mNetworkLocation.hasAccuracy()) {
            if (mGpsLocation.getAccuracy() != mNetworkLocation.getAccuracy()) {
                return mGpsLocation.getAccuracy() < mNetworkLocation.getAccuracy() ? mGpsLocation : mNetworkLocation;
            }
        }
        // Fall back to whichever is more recent by elapsed realtime (immune to clock changes).
        return mGpsLocation.getElapsedRealtimeNanos() >= mNetworkLocation.getElapsedRealtimeNanos()
                ? mGpsLocation : mNetworkLocation;
    }

    private void commitLocation(Location location) {
        mCommittedLocation = location;// null is a valid commit - it means "fell back," and stays open to a real result arriving later
        if (mLocationTimeoutRunnable != null) {
            mLocationTimeoutHandler.removeCallbacks(mLocationTimeoutRunnable);
        }
        finalizeLocation(location);
    }

    /**
     * Applies the winning location (or falls back to cached lat/long if null) and updates the UI.
     */
    private void finalizeLocation(Location location) {
        if (location != null) {
            sLatitude = location.getLatitude();
            sLongitude = location.getLongitude();
            if (sSharedPreferences != null) {
                sSharedPreferences.edit()
                        .putLong("Lat", Double.doubleToRawLongBits(sLatitude))
                        .putLong("Long", Double.doubleToRawLongBits(sLongitude))
                        .apply();
            }
        } else {// if location object is null, we should make another request until it is not null
            if (sLatitude == 0.0 && sLongitude == 0.0) {// get the last location if it exists
                sLatitude = Double.longBitsToDouble(sSharedPreferences.getLong("Lat", 0));
                sLongitude = Double.longBitsToDouble(sSharedPreferences.getLong("Long", 0));
            }
            if (binding != null && binding.shimmerLayout.getVisibility() == View.VISIBLE) {
                mHandler.postDelayed(() -> {
                    if (binding != null) {
                        requestDeviceLocation();
                    }
                }, 500);
            }
        }
        if (mLocationResolver == null) {
            mLocationResolver = new LocationResolver(mContext, mActivity);
        }
        mLocationResolver.resolveCurrentLocationName();
        mLocationResolver.setTimeZoneID();
        showViewAfterLocationCall();
    }

    private void showViewAfterLocationCall() {
        synchronized (sJewishDateInfo) {
            mActivity.runOnUiThread(() -> {
                if (mMainRecyclerView != null && mMainRecyclerView.isFocusable()) {
                    mLocationResolver.getFullLocationName(true, locationName -> {
                        if (locationName == null || locationName.isEmpty()) {//if it's still empty, use backup. NPE was thrown here for some reason
                            locationName = sROZmanimCalendar.getGeoLocation().getLocationName();
                        }
                        sCurrentLocationName = locationName;
                        mActivity.runOnUiThread(() -> {
                            if (binding != null) {
                                binding.dailySummary.dailyLocationName.setText(sCurrentLocationName);
                            }
                        });
                        resolveElevationAndVisibleSunrise(() -> {
                            if (sCurrentDateShown != null) {
                                instantiateZmanimCalendar();
                                sROZmanimCalendar.setLocalDate(sCurrentDateShown);
                                setNextUpcomingZman();
                                createBackgroundThreadForNextUpcomingZman();
                                sJewishDateInfo.setLocalDate(sCurrentDateShown);
                                updateChaitableTimesFromLatLong(() -> {
                                    if (sSharedPreferences.getBoolean("weeklyMode", false)) {
                                        updateWeeklyTextViewTextColor();
                                        updateWeeklyZmanim();
                                    } else {
                                        updateDailyZmanim();
                                        if (mNestedScrollView != null) {
                                            mNestedScrollView.scrollTo(0, mCurrentPosition);
                                        }
                                    }
                                    if (binding != null) {
                                        binding.shimmerLayout.setVisibility(View.GONE);
                                        setWeeklyViewVisibility(sSharedPreferences.getBoolean("weeklyMode", false));
                                    }
                                    if (mCalendarButton != null) {
                                        mCalendarButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, Utils.getCurrentCalendarDrawable(sSettingsPreferences, sCurrentDateShown));
                                    }
                                    setAllNotifications();
                                });
                            }
                        });
                    });
                }
            });
        }
    }
}