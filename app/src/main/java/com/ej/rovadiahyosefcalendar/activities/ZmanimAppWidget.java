package com.ej.rovadiahyosefcalendar.activities;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

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

import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.SecondTreatment;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.ej.rovadiahyosefcalendar.classes.ZmanimFactory;
import com.ej.rovadiahyosefcalendar.classes.ZmanimNames;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

/**
 * Implementation of App Widget functionality.
 */
public class ZmanimAppWidget extends AppWidgetProvider {

    private static SharedPreferences mSharedPreferences;
    private static SharedPreferences mSettingsPreferences;
    private static LocationResolver mLocationResolver;
    private static JewishDateInfo mJewishDateInfo;
    private static ROZmanimCalendar mROZmanimCalendar;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        mLocationResolver = new LocationResolver(context, null);
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mROZmanimCalendar = getROZmanimCalendar();
        if (!mROZmanimCalendar.getGeoLocation().equals(new GeoLocation())) {// if using location, default GeoLocation will be returned. Avoid that
            Log.d("ZmanimAppWidget", "GeoLocation is not default");
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

            String jewishDate = mJewishDateInfo.getJewishCalendar().toString();
            String parsha = mJewishDateInfo.getThisWeeksParsha();
            ZmanListEntry nextUpcomingZman = getNextUpcomingZman(context, appWidgetManager, appWidgetId);
            ZmanimNames zmanimNames = new ZmanimNames(mSharedPreferences.getBoolean("isZmanimInHebrew", false), mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false), mSharedPreferences.getBoolean("isZmanimAmericanized", false));
            String zman = nextUpcomingZman.getTitle()
                .replace(zmanimNames.getHalachaBerurahString(), zmanimNames.getAbbreviatedHalachaBerurahString()
                .replace(zmanimNames.getYalkutYosefString(), zmanimNames.getAbbreviatedYalkutYosefString()));
            String time = Utils.formatZmanTime(context, nextUpcomingZman);
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

    private static ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(null, true));
    }

    public static ZmanListEntry getNextUpcomingZman(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (mROZmanimCalendar == null || mJewishDateInfo == null) {
            mLocationResolver = new LocationResolver(context, null);
            mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            mROZmanimCalendar = getROZmanimCalendar();
            mROZmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
            mROZmanimCalendar.setCandleLightingOffset(Double.parseDouble(mSettingsPreferences.getString("CandleLightingOffset", "20")));
            mROZmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(mSettingsPreferences.getString("EndOfShabbatOffset", "40")));
            mJewishDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));
        }
        if (mSettingsPreferences == null || mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        ZmanListEntry nextZman = ZmanimFactory.getNextUpcomingZman(new GregorianCalendar(), mROZmanimCalendar, mJewishDateInfo, mSettingsPreferences, mSharedPreferences);
        if (nextZman == null || nextZman.getZman() == null) {
            nextZman = new ZmanListEntry("", new Date(System.currentTimeMillis() + 300_000), SecondTreatment.ROUND_EARLIER, "");// try again in 5 minutes
        }
        return nextZman;
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
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

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
