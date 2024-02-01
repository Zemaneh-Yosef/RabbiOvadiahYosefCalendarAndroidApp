package com.ej.rovadiahyosefcalendar.notifications;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

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
import com.ej.rovadiahyosefcalendar.activities.MainActivity;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class NextZmanCountdownNotification extends Service {

    private static final String CHANNEL_ID = "next_zman_countdown_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final long COUNTDOWN_INTERVAL = 1000; // 1 second
    private Handler handler;
    private Runnable countdownRunnable;
    private long remainingTime;
    private long timeTillNextZman;

    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences mSettingsPreferences;
    private static LocationResolver mLocationResolver;
    private static JewishDateInfo mJewishDateInfo;
    private static ROZmanimCalendar mROZmanimCalendar;
    private static boolean mIsZmanimInHebrew;
    private static boolean mIsZmanimEnglishTranslated;
    private ZmanListEntry nextZman;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setZmanimLanguageBools();
        mLocationResolver = new LocationResolver(this, new Activity());
        mROZmanimCalendar = getROZmanimCalendar(this);
        mROZmanimCalendar.setExternalFilesDir(getExternalFilesDir(null));
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        createNotificationChannel();
        startCountdown();
    }

    private static void setZmanimLanguageBools() {
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

    private static ROZmanimCalendar getROZmanimCalendar(Context context) {
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

    private static double getLastKnownElevation(Context context) {
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
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startCountdown() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
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
                    updateNotification(nextZman.getTitle());
                    remainingTime = timeTillNextZman;
                    handler.postDelayed(this, 0);
                } else {
                    updateNotification(nextZman.getTitle());
                    remainingTime -= COUNTDOWN_INTERVAL;
                    handler.postDelayed(this, COUNTDOWN_INTERVAL);
                }
            }
        };

        handler.post(countdownRunnable);
    }

    private void updateNotification(String contentText) {
        long seconds = (remainingTime / 1000) % 60;
        long minutes = (remainingTime / (1000 * 60)) % 60;
        long hours = (remainingTime / (1000 * 60 * 60)) % 24;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_av_timer_24)
                .setContentTitle(contentText)
                .setContentText(String.format(Locale.getDefault(),"%02dh:%02dm:%02ds", hours, minutes, seconds))
                .setProgress((int) timeTillNextZman, (int) remainingTime, false)
                .setOngoing(true);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Next Zman Countdown Channel";
            String description = "Next Zman Countdown Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(countdownRunnable);
        stopForeground(true);
    }
}

