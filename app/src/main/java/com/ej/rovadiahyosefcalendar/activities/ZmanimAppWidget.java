package com.ej.rovadiahyosefcalendar.activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        mROZmanimCalendar = getROZmanimCalendar(context);
        mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
        mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
        mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
        mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
        SimpleDateFormat sZmanimFormat;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            sZmanimFormat = new SimpleDateFormat("H:mm", Locale.getDefault());
        } else {
            sZmanimFormat = new SimpleDateFormat("h:mm aa", Locale.getDefault());
        }
        sZmanimFormat.setTimeZone(mROZmanimCalendar.getCalendar().getTimeZone());

        String jewishDate = mJewishDateInfo.getJewishDate();
        String parsha = mJewishDateInfo.getThisWeeksParsha();
        ZmanListEntry nextUpcomingZman = getNextUpcomingZman(context);
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

        RemoteViews views;
        if (mSharedPreferences.getInt("widgetMaxHeight" + appWidgetId, 0) > mSharedPreferences.getInt("widgetMaxWidth" + appWidgetId, 0)) {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_horizontal);
            Log.d("App widget", "widget max height is greater than max width, setting layout to horizontal");
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
            Log.d("App widget", "widget max height is lesser than max width, setting layout to vertical");
        }
        views.setTextViewText(R.id.jewish_date, jewishDate);
        views.setTextViewText(R.id.parsha, parsha);
        views.setTextViewText(R.id.zman, zman);
        views.setTextViewText(R.id.zman_time, time);
        views.setTextViewText(R.id.tachanun, tachanun);
        views.setTextViewText(R.id.daf, dafYomi);

        if (!mSharedPreferences.getBoolean("widgetInitialized", false)) {
            views.setViewVisibility(R.id.widget_next_zman, View.GONE);// initially hide the other views
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            Log.d("App widget", "widget has just been initialized, hiding other texts");
        }

        Intent configIntent = new Intent(context, MainActivity.class);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget, configPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
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

    public static ZmanListEntry getNextUpcomingZman(Context context) {
        if (mROZmanimCalendar == null || mJewishDateInfo == null) {
            mLocationResolver = new LocationResolver(context, new Activity());
            mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            setZmanimLanguageBools();
            mROZmanimCalendar = getROZmanimCalendar(context);
            mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
            mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
            mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
            mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false), true);
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
            Log.d("App widget", "updating all widgets, this widget's id is: " + appWidgetId);
        }
        scheduleUpdates(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        Log.d("App widget", "Widget has been changed, updating...");

        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        Log.d("App widget", "min width is: " + minWidth);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        Log.d("App widget", "min height is: " + minHeight);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        Log.d("App widget", "max width is: " + maxWidth);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        Log.d("App widget", "max height is: " + maxHeight);

        RemoteViews views;
        if (maxHeight > maxWidth) {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget_horizontal);
            Log.d("App widget", "widget max height is greater than max width, setting layout to horizontal");
        } else {
            views = new RemoteViews(context.getPackageName(), R.layout.zmanim_app_widget);
            Log.d("App widget", "widget max height is lesser than max width, setting layout to vertical");
        }

        if (minWidth >= 250 || maxHeight > maxWidth) {// if the widget is wide enough, show the zman
            views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            Log.d("App widget", "widget is only wide enough to show the date and next upcoming zman");
        } else {
            views.setViewVisibility(R.id.widget_next_zman, View.GONE);
            views.setViewVisibility(R.id.widget_tachanun_daf, View.GONE);
            Log.d("App widget", "widget is only wide enough to show the date");
        }
        if (minWidth >= 350 || maxHeight > maxWidth) {// if the widget is wide enough, show the daf as well
            views.setViewVisibility(R.id.widget_next_zman, View.VISIBLE);
            views.setViewVisibility(R.id.widget_tachanun_daf, View.VISIBLE);
            Log.d("App widget", "widget is wide enough to show everything");
        }

        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSharedPreferences.edit().putInt("widgetMaxWidth" + appWidgetId, maxWidth).apply();
        mSharedPreferences.edit().putInt("widgetMaxHeight" + appWidgetId, maxHeight).apply();
        mSharedPreferences.edit().putBoolean("widgetInitialized", true).apply();

        appWidgetManager.updateAppWidget(appWidgetId, views);

        updateAppWidget(context, appWidgetManager, appWidgetId);
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
                        getNextUpcomingZman(context).getZman().getTime() + 100,
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
