package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.FOREGROUND_SERVICE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sShabbatMode;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.ui.DailySummaryController;
import com.ej.rovadiahyosefcalendar.activities.ui.limudiim.LimudFragment;
import com.ej.rovadiahyosefcalendar.activities.ui.siddur.SiddurFragment;
import com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment;
import com.ej.rovadiahyosefcalendar.classes.ChaiTablesWebJava;
import com.ej.rovadiahyosefcalendar.classes.DailyEntriesBuilder;
import com.ej.rovadiahyosefcalendar.classes.ExceptionHandler;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.NisanLimudYomi;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.databinding.ActivityMainFragmentManagerBinding;
import com.ej.rovadiahyosefcalendar.notifications.NextZmanCountdownNotification;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
import com.kosherjava.zmanim.util.GeoLocation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class MainFragmentManagerActivity extends AppCompatActivity {

    private boolean mBackHasBeenPressed = false;
    /**
     * This string is used to display the name of the current location in the app. We also use this string to save the elevation of a location to the
     * SharedPreferences, and we save the chai tables files under this name as well.
     */
    public static String sCurrentLocationName = "";
    public static double sLatitude;
    public static double sLongitude;
    public static double sElevation = 0;
    public static String sCurrentTimeZoneID = TimeZone.getDefault().getID();//e.g. "America/New_York"

    // KosherJava classes
    public static ROZmanimCalendar sROZmanimCalendar = new ROZmanimCalendar(new GeoLocation());// avoid NPE
    public final static JewishDateInfo sJewishDateInfo = new JewishDateInfo(false);
    public static HebrewDateFormatter sHebrewDateFormatter = new HebrewDateFormatter();

    // Android classes:
    public static SharedPreferences sSharedPreferences;
    public static SharedPreferences sSettingsPreferences;
    public static final String SHARED_PREF = "MyPrefsFile";
    public static ActivityResultLauncher<Intent> sSetupLauncher;
    public static MaterialToolbar materialToolbar;

    /**
     * The current date shown in all activities.
     */
    public static LocalDate sCurrentDateShown = LocalDate.now();
    public static Instant sLastTimeUserWasInApp;
    /**
     * The control used to switch tabs: the bottom bar, or on wide screens in weekly mode, the navigation rail.
     */
    public static NavigationBarView sNavView;
    private final List<NavigationBarView> mNavViews = new ArrayList<>();// every tab control in the layout (the rail only exists on wide screens)
    private BottomNavigationView mBottomNav;
    private NavigationRailView mNavRail;
    private View mSidebarScroll;
    private SharedPreferences.OnSharedPreferenceChangeListener mWeeklyModeListener;
    public static ViewPager2 sViewPager;
    private ViewPager2 mViewPager;
    /**
     * The day summary shown as a sidebar next to the tabs on wide screens (tablets, unfolded foldables). Null on phones.
     */
    private static DailySummaryController sSidebar;
    private static boolean sUpdateChecked = false;

    private final ActivityResultLauncher<IntentSenderRequest> updateLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                // RESULT_OK = accepted, RESULT_CANCELED = declined; nothing to do for a flexible update
            });

    @Override
    protected void attachBaseContext(Context base) {
        String lang = PreferenceManager.getDefaultSharedPreferences(base).getString("language", "Default");
        Locale locale = switch (lang) {
            case "English" -> new Locale.Builder().setLanguage("en").setRegion("US").build();
            case "Hebrew" -> new Locale.Builder().setLanguage("he").setRegion("IL").build();
            default -> null;
        };
        if (locale != null) {
            Locale.setDefault(locale);
            Configuration config = base.getResources().getConfiguration();
            config.setLocale(locale);
            config.setLayoutDirection(locale);
            base = base.createConfigurationContext(config);
        }
        if (Utils.isLocaleHebrew(base)) {
            base.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit()
                    .putBoolean("isZmanimInHebrew", true)
                    .apply();
        }
        super.attachBaseContext(base);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(this);

        ActivityMainFragmentManagerBinding binding = ActivityMainFragmentManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getActionBar() != null) {// only for emulator
            Objects.requireNonNull(getActionBar()).hide();
        }

        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        materialToolbar = binding.topAppBar;
        appBarLayout.setStatusBarForeground(MaterialShapeDrawable.createWithElevationOverlay(this));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        sHebrewDateFormatter.setUseGershGershayim(false);
        sSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        sSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sSharedPreferences.getBoolean("massUpdateCheck", true)) {// since version 23.3, we need to move everyone to AH mode if they are outside of Israel. This should eventually be removed, but far into the future
            if (!sSharedPreferences.getBoolean("inIsrael", false)) {
                sSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", true).apply();
                sSharedPreferences.edit().putBoolean("useElevation", false).apply();
            }
            sSharedPreferences.edit().putBoolean("massUpdateCheck", false).apply();// do not check again
        }

        if (sSharedPreferences.getBoolean("RYYHaskamaNotShown", true)) {
            if (sSharedPreferences.getBoolean("isSetup", false)) {// so.. this is confusing because we do not want to show this dialog to new users, however, new users are not setup yet, so instead we check for if they are setup...
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.new_haskama)
                        .setMessage(R.string.the_team_behind_zemaneh_yosef_is_proud_to_announce_that_we_have_recently_received_a_new_haskama_from_the_rishon_l_tzion_harav_yitzhak_yosef_check_it_out)
                        .setPositiveButton(R.string.haskama_by_rabbi_yitzhak_yosef, (dialog, which) -> Utils.startActivitySafely(this, new Intent(Intent.ACTION_VIEW, Uri.parse("https://royzmanim.com/assets/haskamah-rishon-letzion.pdf"))))
                        .setNegativeButton(R.string.dismiss, (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            }
            sSharedPreferences.edit().putBoolean("RYYHaskamaNotShown", false).apply();// do not check again
        }

        sJewishDateInfo.setLocalDate(LocalDate.now());
        sJewishDateInfo.resetLocale(this);
        sJewishDateInfo.getJewishCalendar().setInIsrael(sSharedPreferences.getBoolean("inIsrael", false));
        initSetupResult();
        if (ChaiTablesWebJava.checkIfFileDoesNotExist(getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.getJewishCalendar().getJewishYear())
                && sSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)
                && !sSharedPreferences.getBoolean("isSetup", false)
                && savedInstanceState == null) {// it should only run if the user has not set up the app
            sSetupLauncher.launch(new Intent(this, WelcomeScreenActivity.class));
            initZmanimNotificationDefaults();
        }

        setSupportActionBar(new MaterialToolbar(this));
        sViewPager = findViewById(R.id.viewPager);
        mViewPager = sViewPager;
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        sViewPager.setAdapter(adapter);
        // Only the tab on screen (the resumed one) can take the D-pad focus. ViewPager2 keeps the tabs next to it laid out off
        // screen, and Android's focus search would otherwise move into them (e.g. pressing left towards the rail from the zmanim
        // went into the limudim).
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment tab, @NonNull View view, @Nullable Bundle savedInstanceState) {
                setTabFocusable(tab, view, false);// until it is the tab on screen
            }

            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment tab) {
                setTabFocusable(tab, tab.getView(), true);
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment tab) {
                setTabFocusable(tab, tab.getView(), false);
            }
        }, false);// only the tabs themselves, not the fragments inside them
        mBottomNav = binding.navView;
        mNavRail = binding.navRail;
        mNavViews.add(mBottomNav);
        if (mNavRail != null) {
            mNavViews.add(mNavRail);
        }
        sNavView = mBottomNav;
        // Set initial page to ZmanimFragment (index 1)
        sViewPager.setCurrentItem(1, false);  // Set to page 1 without smooth scroll

        // Set initial selection on BottomNavigationView to the corresponding menu item
        for (NavigationBarView navView : mNavViews) {
            navView.setSelectedItemId(R.id.navigation_zmanim);
        }

        if (binding.sidebarSummary != null) {// only in the wide-screen layout (layout-w600dp-h480dp)
            setupSidebar(binding);
        } else {
            sSidebar = null;
        }

        if (sJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT) {
           for (NavigationBarView navView : mNavViews) {
               navView.getOrCreateBadge(R.id.navigation_siddur).setText("תפילה לאתרוג");
           }
        }

        if (sJewishDateInfo.getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parshah.BESHALACH &&
                sJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY) {
            for (NavigationBarView navView : mNavViews) {
                navView.getOrCreateBadge(R.id.navigation_siddur).setText("פרשת המן");
            }
        }

        if (sJewishDateInfo.getJewishCalendar().isErevRoshChodesh() &&
                sJewishDateInfo.getJewishCalendar().getJewishMonth() == JewishDate.IYAR) {
            for (NavigationBarView navView : mNavViews) {
                navView.getOrCreateBadge(R.id.navigation_siddur).setText("תפילת השל\"ה");
            }
        }

        if (sJewishDateInfo.getJewishCalendar().getJewishMonth() == JewishDate.NISSAN) {
            String title = NisanLimudYomi.getNisanLimudYomiTitle(sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth());
            if (!title.isEmpty()) {
                for (NavigationBarView navView : mNavViews) {
                    navView.getOrCreateBadge(R.id.navigation_limud).setText(title);
                }
            }
        }

        // Synchronize BottomNavigationView with ViewPager2
        sViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (materialToolbar != null) {
                    if (position == 0) {// Limud
                        materialToolbar.setTitle(getString(R.string.limudim_hillulot));
                        if (Utils.isLocaleHebrew(MainFragmentManagerActivity.this)) {
                            materialToolbar.setSubtitle(getString(R.string.app_name));
                        } else {
                            materialToolbar.setSubtitle(getString(R.string.short_app_name));
                        }
                    } else if (position == 1) {// Zmanim
                        materialToolbar.setTitle(getString(R.string.app_name));
                        if (Utils.isLocaleHebrew(MainFragmentManagerActivity.this)) {
                            materialToolbar.setSubtitle("");
                        }
                    } else {// Siddur
                        materialToolbar.setTitle(getString(R.string.show_siddur));
                        materialToolbar.setSubtitle(getString(R.string.short_app_name));
                    }
                }
                for (NavigationBarView navView : mNavViews) {
                    navView.getMenu().getItem(position).setChecked(true);
                }
                applyWideMode();
            }
        });

        // Set up the BottomNavigationView to change ViewPager2 page on item click
        NavigationBarView.OnItemSelectedListener onTabSelected = item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_limud) {
                sViewPager.setCurrentItem(0, false);
                return true;
            } else if (itemId == R.id.navigation_zmanim) {
                sViewPager.setCurrentItem(1, false);
                return true;
            } else if (itemId == R.id.navigation_siddur) {
                sViewPager.setCurrentItem(2, false);
                return true;
            }
            return false;
        };
        for (NavigationBarView navView : mNavViews) {
            navView.setOnItemSelectedListener(onTabSelected);
        }
        applyWideMode();

        askToUpdateApp();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (sViewPager.getCurrentItem() == 0 || sViewPager.getCurrentItem() == 2) {
                    sViewPager.setCurrentItem(1);
                    return;
                }
                if (!mBackHasBeenPressed) {
                    mBackHasBeenPressed = true;
                    Toast.makeText(MainFragmentManagerActivity.this, R.string.press_back_again_to_close_the_app, Toast.LENGTH_SHORT).show();
                    return;
                }
                finish();
            }
        });
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
                    sJewishDateInfo.getJewishCalendar().setInIsrael(sSharedPreferences.getBoolean("inIsrael", false));
                    sElevation = Utils.parseDoubleOrDefault(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"), 0);
                }
        );
    }

    private void initZmanimNotificationDefaults() {
        sSettingsPreferences.edit()
                .putBoolean("zmanim_notifications", true)
                .putInt("autoDismissNotifications", -1)
                .putInt("Alot", -1)
                .putInt("TalitTefilin", -1)
                .putInt("HaNetz", -1)
                .putInt("SofZmanShmaMGA", 15)
                .putInt("SofZmanShmaGRA", 15)
                .putInt("SofZmanTefila", 15)
                .putInt("SofZmanAchilatChametz", 15)
                .putInt("SofZmanBiurChametz", 15)
                .putInt("Chatzot", 20)
                .putInt("MinchaGedola", -1)
                .putInt("MinchaKetana", -1)
                .putInt("PlagHaMinchaYY", -1)
                .putInt("PlagHaMinchaHB", -1)
                .putInt("CandleLighting", 15)
                .putInt("Shkia", 15)
                .putInt("TzeitHacochavim", 15)
                .putInt("TzeitHacochavimLChumra", -1)
                .putInt("FastEnd", 15)
                .putInt("ShabbatEnd", -1)
                .putInt("RT", 0)
                .putInt("NightChatzot", -1).apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sNavView != null && sViewPager != null) {
            if (sSettingsPreferences != null && sSettingsPreferences.getBoolean("hideBottomBar", false)) {
                sNavView.setVisibility(View.GONE);
                if (sSidebar == null) {// on wide screens, the row holding the sidebar and the tabs already handles the insets
                    ViewCompat.setOnApplyWindowInsetsListener(sViewPager, (v, windowInsets) -> {
                        Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                        mlp.leftMargin = insets.left;
                        mlp.bottomMargin = insets.bottom;
                        mlp.rightMargin = insets.right;
                        v.setLayoutParams(mlp);
                        // Return CONSUMED if you don't want want the window insets to keep passing
                        // down to descendant views.
                        return WindowInsetsCompat.CONSUMED;
                    });
                }
            } else {
                if (!sShabbatMode) {
                    sNavView.setVisibility(View.VISIBLE);
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) sViewPager.getLayoutParams();
                    mlp.leftMargin = 0;
                    mlp.rightMargin = 0;
                    mlp.bottomMargin = 0;
                    sViewPager.setLayoutParams(mlp);
                }
            }
        }
        ExceptionHandler.isAppFocused = true;
        stopService(new Intent(this, NextZmanCountdownNotification.class));
        if (sSettingsPreferences.getBoolean("showNextZmanNotification", false)) {
            boolean hasForegroundServiceLocation = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||// the permission only exists on android 14 and up
                    ContextCompat.checkSelfPermission(this, FOREGROUND_SERVICE_LOCATION) == PERMISSION_GRANTED;
            if ((ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) ||
                    !hasForegroundServiceLocation) {
                Toast.makeText(this, R.string.no_location_permission_for_next_zman_notification, Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        startForegroundService(new Intent(this, NextZmanCountdownNotification.class));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } else {
                    startForegroundService(new Intent(this, NextZmanCountdownNotification.class));
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ExceptionHandler.isAppFocused = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidget();
        if (sSidebar != null) {
            sSidebar.reloadMakamNames();// in case the language changed
        }
        refreshSidebar();// in case the location or settings changed in another screen
    }

    @Override
    protected void onDestroy() {
        if (sViewPager == mViewPager) {// on a recreate the new instance has already replaced these
            materialToolbar = null;
            sNavView = null;
            sViewPager = null;
            sSidebar = null;
            sSetupLauncher = null;
        }
        if (mWeeklyModeListener != null) {
            sSharedPreferences.unregisterOnSharedPreferenceChangeListener(mWeeklyModeListener);
        }
        super.onDestroy();
    }

    private static void setTabFocusable(Fragment fragment, @Nullable View tabView, boolean focusable) {
        boolean isTab = fragment instanceof ZmanimFragment || fragment instanceof LimudFragment || fragment instanceof SiddurFragment;// not e.g. the date pickers
        if (isTab && tabView instanceof ViewGroup tabRoot) {
            tabRoot.setDescendantFocusability(focusable ? ViewGroup.FOCUS_BEFORE_DESCENDANTS : ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
    }

    /**
     * Lets the D-pad leave the tab sideways, e.g. from the first day of the week to the navigation rail, or to the sidebar.
     * The ViewPager2 swallows left/right itself: it looks for the next page in that direction, and since that page isn't
     * focusable it keeps the focus where it is, so nothing outside the pager is ever reached.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        int keyCode = event.getKeyCode();
        if (event.getAction() != KeyEvent.ACTION_DOWN || (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT)) {
            return false;
        }
        View focused = getCurrentFocus();
        if (focused == null || mViewPager == null || !isInside(mViewPager, focused)) {
            return false;
        }
        int direction = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? View.FOCUS_LEFT : View.FOCUS_RIGHT;
        View next = FocusFinder.getInstance().findNextFocus((ViewGroup) getWindow().getDecorView(), focused, direction);
        if (next != null && !isInside(mViewPager, next)) {
            return next.requestFocus(direction);
        }
        return false;
    }

    private static boolean isInside(View parent, View view) {
        for (View v = view; v != null; v = v.getParent() instanceof View p ? p : null) {
            if (v == parent) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets up the day summary sidebar of the wide-screen layout, and keeps it clear of the system bars.
     */
    private void setupSidebar(ActivityMainFragmentManagerBinding binding) {
        sSidebar = new DailySummaryController(this, Objects.requireNonNull(binding.sidebarSummary), true);
        mSidebarScroll = binding.sidebarScroll;
        // "Change date" in the date card's dialog opens the date picker of the tab that is currently shown
        sSidebar.setOnChangeDateListener(() -> {
            Fragment currentTab = getSupportFragmentManager().findFragmentByTag("f" + mViewPager.getCurrentItem());// FragmentStateAdapter's tag for each page
            if (currentTab != null && currentTab.getView() != null) {
                View calendarButton = currentTab.getView().findViewById(R.id.calendar);
                if (calendarButton != null) {
                    calendarButton.performClick();
                }
            }
        });
        if (binding.twoPaneRow != null) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.twoPaneRow, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(insets.left, 0, insets.right, insets.bottom);
                return windowInsets;
            });
            // the row's padding above already keeps the tab controls clear of the system bars, so they shouldn't add their own
            for (NavigationBarView navView : mNavViews) {
                ViewCompat.setOnApplyWindowInsetsListener(navView, (v, windowInsets) -> WindowInsetsCompat.CONSUMED);
            }
        }
        mWeeklyModeListener = (prefs, key) -> {
            if ("weeklyMode".equals(key)) {
                applyWideMode();
            }
        };
        sSharedPreferences.registerOnSharedPreferenceChangeListener(mWeeklyModeListener);
    }

    /**
     * On wide screens, weekly mode shows a whole week in the zmanim and limudim tabs, so the one-day sidebar is only shown next
     * to the siddur, and the tabs move to the navigation rail to leave more height for the week. Daily mode uses the sidebar and
     * the tab bar on top of the content. Does nothing on phones.
     */
    private void applyWideMode() {
        if (sSidebar == null || mNavRail == null || mSidebarScroll == null) {
            return;
        }
        boolean weekly = sSharedPreferences.getBoolean("weeklyMode", false);
        boolean wasHidden = sNavView != null && sNavView.getVisibility() != View.VISIBLE;// hidden by Shabbat mode or the "hide bottom bar" setting
        NavigationBarView active = weekly ? mNavRail : mBottomNav;
        NavigationBarView inactive = weekly ? mBottomNav : mNavRail;
        inactive.setVisibility(View.GONE);
        active.setVisibility(wasHidden ? View.GONE : View.VISIBLE);
        sNavView = active;

        boolean showSidebar = !weekly || mViewPager.getCurrentItem() == 2;// the siddur is always for one day
        boolean sidebarWasShown = mSidebarScroll.getVisibility() == View.VISIBLE;
        mSidebarScroll.setVisibility(showSidebar ? View.VISIBLE : View.GONE);
        if (showSidebar && !sidebarWasShown) {
            refreshSidebar();
        }
    }

    /**
     * Redraws the day summary sidebar (wide screens only) for the date currently shown. Every tab calls this after it changes
     * the shared date (sCurrentDateShown), so that the sidebar always matches the tab next to it.
     */
    public static void refreshSidebar() {
        if (sSidebar == null || (sLatitude == 0 && sLongitude == 0)) {// no sidebar, or the location is not known yet
            return;
        }
        sSidebar.setLocationName(sCurrentLocationName);
        sSidebar.refresh(DailyEntriesBuilder.infoOnly(DailyEntriesBuilder.build(sSidebar.getContext(), false)));
        sSidebar.applyThemeColors();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, ZmanimAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        if (appWidgetManager != null) {
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), ZmanimAppWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            if (sShabbatMode) {// TODO test
                // Use a Handler to post a runnable to bring the window back to focus
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    getWindow().getDecorView().requestFocus();// Request focus for the window
                    // Optionally, bring the window to the front
                    //getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    // Optionally, you can also use this code to bring the app to the front
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(intent);
                    }
                }, 100); // Delay to ensure the window is in a stable state
            }
        }
    }

    private void askToUpdateApp() {
        if (sUpdateChecked) return;// don't re-prompt on every recreate
        sUpdateChecked = true;
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateLauncher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build());
            }
        });
    }

    // ViewPagerAdapter for managing fragments
    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 1 -> new ZmanimFragment();
                case 2 -> new SiddurFragment();
                default -> new LimudFragment();
            };
        }

        @Override
        public int getItemCount() {
            return 3; // Number of fragments
        }
    }
}