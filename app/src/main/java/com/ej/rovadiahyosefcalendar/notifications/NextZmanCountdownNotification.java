package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class NextZmanCountdownNotification extends Service {

    private static final String CHANNEL_ID = "next_zman_countdown_channel";
    private static final int NOTIFICATION_ID = 1000;
    private static final long COUNTDOWN_INTERVAL = 1000; // 1 second
    private Handler handler;
    private Runnable countdownRunnable;
    private long remainingTime;
    private long timeTillNextZman;

    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    private LocationResolver mLocationResolver;
    private JewishDateInfo mJewishDateInfo;
    private ROZmanimCalendar mROZmanimCalendar;
    private boolean mIsZmanimInHebrew;
    private boolean mIsZmanimEnglishTranslated;

    private boolean shouldShowNotification = true;
    private ZmanListEntry nextZman;
    private DateFormat zmanimFormat;
    private SharedPreferences.OnSharedPreferenceChangeListener settingsPrefListener;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefListener;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        sharedPrefListener = (prefs, key) -> {
            if (key.equals("isZmanimInHebrew") || key.equals("isZmanimEnglishTranslated")) {
                setZmanimLanguageBools();
                nextZman = ZmanimFactory.getNextUpcomingZman(
                        new GregorianCalendar(),
                        mROZmanimCalendar,
                        mJewishDateInfo,
                        mSettingsPreferences,
                        mSharedPreferences,
                        mIsZmanimInHebrew,
                        mIsZmanimEnglishTranslated);
            }
        };
        mSharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        settingsPrefListener = (prefs, key) -> {
            if (key.equals("showNextZmanNotification")) {
                shouldShowNotification = !shouldShowNotification;
            }
        };
        mSettingsPreferences.registerOnSharedPreferenceChangeListener(settingsPrefListener);
        setZmanimLanguageBools();
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
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))); //set the formatters time zone
        mLocationResolver = new LocationResolver(this, new Activity());
        mROZmanimCalendar = getROZmanimCalendar(this);
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
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        createNotificationChannel();
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

    private ROZmanimCalendar getROZmanimCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData();
            if (mLocationResolver.getLatitude() != 0 && mLocationResolver.getLongitude() != 0) {
                return new ROZmanimCalendar(new GeoLocation(
                        mLocationResolver.getLocationName(),
                        mLocationResolver.getLatitude(),
                        mLocationResolver.getLongitude(),
                        getLastKnownElevation(context),
                        mLocationResolver.getTimeZone()));
            }
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(context),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
    }

    private double getLastKnownElevation(Context context) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationResolver.getLocationName(), "0"));//get the elevation using the location name
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        startCountdown();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startCountdown() {
        if (countdownRunnable == null) {
            countdownRunnable = new Runnable() {
                @Override
                public void run() {
                    if (shouldShowNotification) {
                        if (remainingTime <= 0) {
                            nextZman = ZmanimFactory.getNextUpcomingZman(
                                    new GregorianCalendar(),
                                    mROZmanimCalendar,
                                    mJewishDateInfo,
                                    mSettingsPreferences,
                                    mSharedPreferences,
                                    mIsZmanimInHebrew,
                                    mIsZmanimEnglishTranslated);
                            timeTillNextZman = nextZman.getZman().getTime() - new Date().getTime();
                            updateNotification(nextZman);
                            remainingTime = timeTillNextZman;
                            handler.postDelayed(this, 0);
                        } else {
                            updateNotification(nextZman);
                            remainingTime -= COUNTDOWN_INTERVAL;
                            handler.postDelayed(this, COUNTDOWN_INTERVAL);
                        }
                    } else {
                        dismissNotification();
                    }
                }
            };

            handler.post(countdownRunnable);
        }
    }

    private void updateNotification(ZmanListEntry zman) {
        long seconds = (remainingTime / 1000) % 60;
        long minutes = (remainingTime / (1000 * 60)) % 60;
        long hours = (remainingTime / (1000 * 60 * 60)) % 24;

        String text;
        if (mIsZmanimInHebrew) {
            text = String.format("%s : %s", zmanimFormat.format(zman.getZman()), zman.getTitle());
        } else {
            text = zman.getTitle() + " is at " + zmanimFormat.format(zman.getZman());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_av_timer_24)
                .setContentTitle(text)
                .setSubText(mROZmanimCalendar.getGeoLocation().getLocationName())
                .setSilent(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setWhen(System.currentTimeMillis())
                .setContentText(String.format(Locale.getDefault(),"%02dh:%02dm:%02ds", hours, minutes, seconds))
                .setProgress((int) timeTillNextZman, (int) remainingTime, false)
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MainFragmentManager.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        startForeground(NOTIFICATION_ID, notification);
    }

    private void dismissNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String description = "Next Zman Countdown Channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, description, importance);
            channel.setDescription(description);
            channel.enableVibration(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPrefListener);
        mSettingsPreferences.unregisterOnSharedPreferenceChangeListener(settingsPrefListener);
        handler.removeCallbacks(countdownRunnable);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
}

