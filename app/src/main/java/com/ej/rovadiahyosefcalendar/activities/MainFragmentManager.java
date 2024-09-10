package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sShabbatMode;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.ui.limudiim.LimudFragment;
import com.ej.rovadiahyosefcalendar.activities.ui.siddur.SiddurFragment;
import com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment;
import com.ej.rovadiahyosefcalendar.classes.ChaiTables;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.databinding.ActivityMainFragmentManagerBinding;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class MainFragmentManager extends AppCompatActivity {

    private boolean mBackHasBeenPressed = false;
    /**
     * This string is used to display the name of the current location in the app. We also use this string to save the elevation of a location to the
     * SharedPreferences, and we save the chai tables files under this name as well.
     */
    public static String sCurrentLocationName = "";
    public static double sLatitude;
    public static double sLongitude;
    public static double sElevation = 0;
    public static String sCurrentTimeZoneID;//e.g. "America/New_York"

    //custom classes/kosherjava classes:
    private LocationResolver mLocationResolver;
    public static ROZmanimCalendar mROZmanimCalendar;
    public static JewishDateInfo mJewishDateInfo;
    public static HebrewDateFormatter mHebrewDateFormatter = new HebrewDateFormatter();

    //android classes:
    public static SharedPreferences sSharedPreferences;
    public static SharedPreferences sSettingsPreferences;
    public static final String SHARED_PREF = "MyPrefsFile";
    public static ActivityResultLauncher<Intent> sSetupLauncher;
    public static MaterialToolbar materialToolbar;

    /**
     * The current date shown in the main activity.
     */
    public static Calendar mCurrentDateShown = Calendar.getInstance();

    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    public final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    public final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);
    public static Date sLastTimeUserWasInApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        mHebrewDateFormatter.setUseGershGershayim(false);
        sSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        sSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String lang = sSettingsPreferences.getString("language", "Default");
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
        mLocationResolver = new LocationResolver(this, this);
        mJewishDateInfo = new JewishDateInfo(sSharedPreferences.getBoolean("inIsrael", false));
        initSetupResult();
        if (ChaiTables.visibleSunriseFileDoesNotExist(getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar())
                && sSharedPreferences.getBoolean("UseTable" + sCurrentLocationName, true)
                && !sSharedPreferences.getBoolean("isSetup", false)
                && savedInstanceState == null) {//it should only not exist the first time running the app and only if the user has not set up the app
            sSetupLauncher.launch(new Intent(this, FullSetupActivity.class));
            initZmanimNotificationDefaults();
        } else {
            mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
        }
        updateWidget();

        setSupportActionBar(new MaterialToolbar(this));
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Set initial page to ZmanimFragment (index 1)
        viewPager.setCurrentItem(1, false);  // Set to page 1 without smooth scroll

        // Set initial selection on BottomNavigationView to the corresponding menu item
        navView.setSelectedItemId(R.id.navigation_zmanim);

        if (mJewishDateInfo.getJewishCalendar().getYomTovIndex() == JewishCalendar.TU_BESHVAT ||
                (mJewishDateInfo.getJewishCalendar().getUpcomingParshah() == JewishCalendar.Parsha.BESHALACH &&
                mJewishDateInfo.getJewishCalendar().getDayOfWeek() == Calendar.TUESDAY)) {
           navView.getOrCreateBadge(R.id.navigation_siddur).setNumber(1);
        }

        // Synchronize BottomNavigationView with ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (materialToolbar != null) {
                    if (position == 0) {// Limud
                        materialToolbar.setTitle(getString(R.string.limudim_hillulot));
                        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            materialToolbar.setSubtitle(getString(R.string.app_name));
                        } else {
                            materialToolbar.setSubtitle(getString(R.string.short_app_name));
                        }
                    } else if (position == 1) {// Zmanim
                        if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                            materialToolbar.setTitle(getString(R.string.app_name));
                            materialToolbar.setSubtitle("");
                        } else {
                            materialToolbar.setTitle(getString(R.string.app_name));
                        }
                    } else {// Siddur
                        materialToolbar.setTitle(getString(R.string.show_siddur));
                        materialToolbar.setSubtitle(getString(R.string.short_app_name));
                    }
                }
                navView.getMenu().getItem(position).setChecked(true);
            }
        });

        // Set up the BottomNavigationView to change ViewPager2 page on item click
        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_limud) {
                viewPager.setCurrentItem(0, false);
                return true;
            } else if (itemId == R.id.navigation_zmanim) {
                viewPager.setCurrentItem(1, false);
                return true;
            } else if (itemId == R.id.navigation_siddur) {
                viewPager.setCurrentItem(2, false);
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == 0 || viewPager.getCurrentItem() == 2) {
                    viewPager.setCurrentItem(1);
                    return;
                }
                if (!mBackHasBeenPressed) {
                    mBackHasBeenPressed = true;
                    Toast.makeText(MainFragmentManager.this, R.string.press_back_again_to_close_the_app, Toast.LENGTH_SHORT).show();
                    return;
                }
                finish();
            }
        });
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
                    sElevation = Double.parseDouble(sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        if (result.getData() != null) {
//
//                        }
//                    }
//                    if (!mInitialized) {
//                        initMainView();
//                    }
//                    instantiateZmanimCalendar();
//                    setZmanimLanguageBools();
//                    if (mSharedPreferences.getBoolean("weeklyMode", false)) {
//                        updateWeeklyZmanim();
//                    } else {
//                        updateDailyZmanim();
//                    }

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

    private void initZmanimNotificationDefaults() {
        sSettingsPreferences.edit().putBoolean("zmanim_notifications", true).apply();
        sSettingsPreferences.edit().putInt("autoDismissNotifications", -1).apply();
        sSettingsPreferences.edit().putInt("Alot", -1).apply();
        sSettingsPreferences.edit().putInt("TalitTefilin", -1).apply();
        sSettingsPreferences.edit().putInt("HaNetz", -1).apply();
        sSettingsPreferences.edit().putInt("SofZmanShmaMGA", 15).apply();
        sSettingsPreferences.edit().putInt("SofZmanShmaGRA", 15).apply();
        sSettingsPreferences.edit().putInt("SofZmanTefila", 15).apply();
        sSettingsPreferences.edit().putInt("SofZmanAchilatChametz", 15).apply();
        sSettingsPreferences.edit().putInt("SofZmanBiurChametz", 15).apply();
        sSettingsPreferences.edit().putInt("Chatzot", 20).apply();
        sSettingsPreferences.edit().putInt("MinchaGedola", -1).apply();
        sSettingsPreferences.edit().putInt("MinchaKetana", -1).apply();
        sSettingsPreferences.edit().putInt("PlagHaMinchaYY", -1).apply();
        sSettingsPreferences.edit().putInt("PlagHaMinchaHB", -1).apply();
        sSettingsPreferences.edit().putInt("CandleLighting", 15).apply();
        sSettingsPreferences.edit().putInt("Shkia", 15).apply();
        sSettingsPreferences.edit().putInt("TzeitHacochavim", 15).apply();
        sSettingsPreferences.edit().putInt("FastEnd", 15).apply();
        sSettingsPreferences.edit().putInt("FastEndStringent", 15).apply();
        sSettingsPreferences.edit().putInt("ShabbatEnd", -1).apply();
        sSettingsPreferences.edit().putInt("RT", 0).apply();
        sSettingsPreferences.edit().putInt("NightChatzot", -1).apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidget();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, ZmanimAppWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), ZmanimAppWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
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

    // ViewPagerAdapter for managing fragments
    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new ZmanimFragment();
                case 2:
                    return new SiddurFragment();
                default:
                    return new LimudFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of fragments
        }
    }
}