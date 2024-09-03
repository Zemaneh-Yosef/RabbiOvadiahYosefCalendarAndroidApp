package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Implementation of App Widget functionality.
 */
public class ZmanimAppWidget extends AppWidgetProvider {

    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences mSettingsPreferences;
    private static LocationResolver mLocationResolver;
    private static JewishDateInfo mJewishDateInfo;
    private static ROZmanimCalendar mROZmanimCalendar;
    private static boolean mIsZmanimInHebrew;
    private static boolean mIsZmanimEnglishTranslated;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mLocationResolver = new LocationResolver(context, new Activity());
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        setZmanimLanguageBools();
        mROZmanimCalendar = getROZmanimCalendar(context, appWidgetManager, appWidgetId);
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
            mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
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
            SimpleDateFormat sZmanimFormat;
            if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                sZmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
            } else {
                sZmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
            }
            sZmanimFormat.setTimeZone(mROZmanimCalendar.getCalendar().getTimeZone());

            String jewishDate = mJewishDateInfo.getJewishDate();
            String parsha = mJewishDateInfo.getThisWeeksParsha();
            ZmanListEntry nextUpcomingZman = getNextUpcomingZman(context, appWidgetManager, appWidgetId);
            String zman = nextUpcomingZman.getTitle();
            String time = sZmanimFormat.format(nextUpcomingZman.getZman());
            String tachanun = mJewishDateInfo.getIsTachanunSaid()
                    .replace("No Tachanun today", "No Tachanun")
                    .replace("Tachanun only in the morning", "Tachanun morning only")
                    .replace("There is Tachanun today", "Tachanun");
            HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
            hebrewDateFormatter.setUseGershGershayim(false);
            String dafYomi = mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getMasechta()
                    + " " + hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getDaf());
            String omerCount = mJewishDateInfo.addDayOfOmer("");

            Map<SizeF, RemoteViews> viewMapping = getViewMapping(context, true, jewishDate, parsha, zman, time, tachanun, dafYomi, omerCount);
            RemoteViews views;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                views = new RemoteViews(viewMapping);
            } else { // old implementation
                if (mSharedPreferences.getInt("widgetMaxHeight" + appWidgetId, 0) > mSharedPreferences.getInt("widgetMaxWidth" + appWidgetId, 0)) {
                    views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_vertical);
                } else {
                    views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
                }
                setTexts(context, views, jewishDate, parsha, zman, time, tachanun, dafYomi);
                if (!mSharedPreferences.getBoolean("widgetInitialized", false)) {
                    views.setViewVisibility(R.id.widget_next_zman, View.GONE);// initially hide the other views
                    views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
                }
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private static void setTexts(Context context, RemoteViews views, String jewishDate, String parsha, String zman, String time, String tachanun, String dafYomi) {
        views.setTextViewText(R.id.jewish_date, jewishDate);
        views.setTextViewText(R.id.parsha, parsha);
        views.setTextViewText(R.id.zman, zman);
        views.setTextViewText(R.id.zman_time, time);
        views.setTextViewText(R.id.tachanun, tachanun);
        views.setTextViewText(R.id.daf, dafYomi);
        views.setOnClickPendingIntent(R.id.widget, PendingIntent.getActivity(context, 0, new Intent(context, MainFragmentManager.class), PendingIntent.FLAG_IMMUTABLE));
    }

    private static ROZmanimCalendar getROZmanimCalendar(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            mLocationResolver.getRealtimeNotificationData(location -> {
                mROZmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                        mLocationResolver.getLocationName(location.getLatitude(), location.getLongitude()),
                        location.getLatitude(),
                        location.getLongitude(),
                        getLastKnownElevation(context, location.getLatitude(), location.getLongitude()),
                        mLocationResolver.getTimeZone()));
                mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
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
                SimpleDateFormat sZmanimFormat;
                if (Locale.getDefault().getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                    sZmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
                } else {
                    sZmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
                }
                sZmanimFormat.setTimeZone(mROZmanimCalendar.getCalendar().getTimeZone());

                String jewishDate = mJewishDateInfo.getJewishDate();
                String parsha = mJewishDateInfo.getThisWeeksParsha();
                ZmanListEntry nextUpcomingZman = getNextUpcomingZman(context, appWidgetManager, appWidgetId);
                String zman = nextUpcomingZman.getTitle();
                String time = sZmanimFormat.format(nextUpcomingZman.getZman());
                String tachanun = mJewishDateInfo.getIsTachanunSaid()
                        .replace("No Tachanun today", "No Tachanun")
                        .replace("Tachanun only in the morning", "Tachanun morning only")
                        .replace("There is Tachanun today", "Tachanun");
                HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();
                hebrewDateFormatter.setUseGershGershayim(false);
                String dafYomi = mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getMasechta()
                        + " " + hebrewDateFormatter.formatHebrewNumber(mJewishDateInfo.getJewishCalendar().getDafYomiBavli().getDaf());
                String omerCount = mJewishDateInfo.addDayOfOmer("");

                Map<SizeF, RemoteViews> viewMapping = getViewMapping(context, true, jewishDate, parsha, zman, time, tachanun, dafYomi, omerCount);
                RemoteViews views;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    views = new RemoteViews(viewMapping);
                } else { // old implementation
                    if (mSharedPreferences.getInt("widgetMaxHeight" + appWidgetId, 0) > mSharedPreferences.getInt("widgetMaxWidth" + appWidgetId, 0)) {
                        views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_vertical);
                    } else {
                        views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
                    }
                    setTexts(context, views, jewishDate, parsha, zman, time, tachanun, dafYomi);
                    if (!mSharedPreferences.getBoolean("widgetInitialized", false)) {
                        views.setViewVisibility(R.id.widget_next_zman, View.GONE);// initially hide the other views
                        views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
                    }
                }
                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views);
            });
        }
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("long", 0)),
                getLastKnownElevation(context, Double.longBitsToDouble(mSharedPreferences.getLong("lat", 0)), Double.longBitsToDouble(mSharedPreferences.getLong("long", 0))),
                TimeZone.getTimeZone(mSharedPreferences.getString("timezoneID", ""))));
    }

    private static double getLastKnownElevation(Context context, double latitude, double longitude) {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else if (ActivityCompat.checkSelfPermission(context, ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED) {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationResolver.getLocationName(latitude, longitude), "0"));//get the elevation using the location name
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return elevation;
    }

    public static ZmanListEntry getNextUpcomingZman(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (mROZmanimCalendar == null || mJewishDateInfo == null) {
            mLocationResolver = new LocationResolver(context, new Activity());
            mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            setZmanimLanguageBools();
            mROZmanimCalendar = getROZmanimCalendar(context, appWidgetManager, appWidgetId);
            mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
            mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
            mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
            mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        }
        return ZmanimFactory.getNextUpcomingZman(new GregorianCalendar(), mROZmanimCalendar, mJewishDateInfo, mSettingsPreferences, mSharedPreferences, mIsZmanimInHebrew, mIsZmanimEnglishTranslated);
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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleUpdates(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        Log.d("App widget", "MIN Width: " + minWidth);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        Log.d("App widget", "MAX Width: " + maxWidth);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        Log.d("App widget", "MIN Height: " + minHeight);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        Log.d("App widget", "MAX Height: " + maxHeight);

        Map<SizeF, RemoteViews> viewMapping = getViewMapping(context,
                false, null, null, null, null, null, null, null);
        RemoteViews views;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views = new RemoteViews(viewMapping);
        } else {
            if (maxHeight > maxWidth) {
                views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_vertical);
            } else {
                views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
            }
            if (minWidth >= 250 || maxHeight > maxWidth) {// if the widget is wide enough, show the zman
                views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);
                views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            } else {
                views.setViewVisibility(R.id.widget_next_zman, View.GONE);
                views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            }
            if (minWidth >= 350 || maxHeight > maxWidth) {// if the widget is wide enough, show the daf as well
                views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);
                views.setViewVisibility(R.id.widget_tachanun_daf, View.VISIBLE);
            }
            mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            mSharedPreferences.edit().putInt("widgetMaxWidth" + appWidgetId, maxWidth).apply();
            mSharedPreferences.edit().putInt("widgetMaxHeight" + appWidgetId, maxHeight).apply();
            mSharedPreferences.edit().putBoolean("widgetInitialized", true).apply();
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    public static Map<SizeF, RemoteViews> getViewMapping(Context context, boolean shouldSetTexts, String jewishDate, String parsha, String zman, String time, String tachanun, String dafYomi, String omer) {
        Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>();
        RemoteViews small = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_small);
        RemoteViews smallTall = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_small_tall);
        RemoteViews mediumTall = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_medium_tall);
        RemoteViews tall = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_vertical);
        RemoteViews veryTall = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_very_tall);
        RemoteViews wide = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
        RemoteViews wideTall = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_tall);
        if (shouldSetTexts) {
            setTexts(context, small, jewishDate, parsha, zman, time, tachanun, dafYomi);
            setTexts(context, smallTall, jewishDate, parsha, zman, time, tachanun, dafYomi);
            setTexts(context, mediumTall, jewishDate, parsha, zman, time, tachanun, dafYomi);
            setTexts(context, tall, jewishDate, parsha, zman, time, tachanun, dafYomi);
            setTexts(context, veryTall, jewishDate, parsha, zman, time, tachanun, dafYomi);
            if (omer.isEmpty()) {
                wideTall.setViewVisibility(R.id.widget_omer_layout, View.GONE);
                veryTall.setViewVisibility(R.id.widget_omer_layout, View.GONE);
            } else {
                wideTall.setViewVisibility(R.id.widget_omer_layout, View.VISIBLE);
                wideTall.setTextViewText(R.id.widget_omer_count, omer);
                veryTall.setViewVisibility(R.id.widget_omer_layout, View.VISIBLE);
                veryTall.setTextViewText(R.id.widget_omer_count, omer);
            }
            setTexts(context, wide, jewishDate, parsha, zman, time, tachanun, dafYomi);
            setTexts(context, wideTall, jewishDate, parsha, zman, time, tachanun, dafYomi);
        }
        // not that wide, but tall views
        viewMapping.put(new SizeF(50f, 50f), small);
        viewMapping.put(new SizeF(50f, 100f), smallTall);
        viewMapping.put(new SizeF(50f, 200f), mediumTall);
        viewMapping.put(new SizeF(50f, 300f), tall);
        // wide
        viewMapping.put(new SizeF(200f, 50f), wide);
        viewMapping.put(new SizeF(200f, 125f), wideTall);
        // wide and tall views
        viewMapping.put(new SizeF(150f, 200f), tall);
        viewMapping.put(new SizeF(150f, 350f), veryTall);
        viewMapping.put(new SizeF(250f, 300f), veryTall);
        return viewMapping;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putBoolean("widgetInitialized", false).apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putBoolean("widgetInitialized", false).apply();
        cancelUpdates(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        scheduleUpdates(context);
    }

    private int[] getActiveWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ZmanimAppWidget.class);

        // return ID of all active widgets within this AppWidgetProvider
        return appWidgetManager.getAppWidgetIds(componentName);
    }

    private void scheduleUpdates(Context context) {
        int[] activeWidgetIds = getActiveWidgetIds(context);

        if (activeWidgetIds.length > 0) {
            PendingIntent pendingIntent = getUpdatePendingIntent(context);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        getNextUpcomingZman(context, AppWidgetManager.getInstance(context), activeWidgetIds[0]).getZman().getTime() + 100,
                        pendingIntent
                );
            }
        }
    }

    private PendingIntent getUpdatePendingIntent(Context context) {
        Class<?> widgetClass = ZmanimAppWidget.class;
        int[] widgetIds = getActiveWidgetIds(context);
        Intent updateIntent = new Intent(context, widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        int requestCode = widgetClass.getName().hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        return PendingIntent.getBroadcast(context, requestCode, updateIntent, flags);
    }

    private void cancelUpdates(Context context) {
        PendingIntent pendingIntent = getUpdatePendingIntent(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

}
