package com.ej.rovadiahyosefcalendar.notifications;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.classes.ZmanimFactory.addZmanim;
import static com.ej.rovadiahyosefcalendar.notifications.NotificationUtils.setExactAndAllowWhileIdle;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;

import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.ROZmanimCalendar;
import com.ej.rovadiahyosefcalendar.classes.ZmanListEntry;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.function.Consumer;

public class ZmanimNotifications extends BroadcastReceiver implements Consumer<Location> {

    private SharedPreferences mSharedPreferences;
    private SharedPreferences mSettingsPreferences;
    private LocationResolver mLocationResolver;
    private Context context;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mLocationResolver = new LocationResolver(context, null);
        if (mSharedPreferences.getBoolean("isSetup",false) && mSettingsPreferences.getBoolean("zmanim_notifications", true)) {
            Thread thread = new Thread(() -> {
                ROZmanimCalendar zmanimCalendar = getROZmanimCalendar();
                if (!zmanimCalendar.getGeoLocation().equals(new GeoLocation())) {// if equal, we can get the user's location in the accept method
                    JewishDateInfo jDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael",false));
                    zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
                    String candles = mSettingsPreferences.getString("CandleLightingOffset", "20");
                    if (candles.isEmpty()) {
                        candles = "20";
                    }
                    zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
                    String shabbat = mSettingsPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
                    if (shabbat.isEmpty()) {// for some reason this is happening
                        shabbat = "40";
                    }
                    zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
                    if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                        zmanimCalendar.setAteretTorahSunsetOffset(30);
                    }
                    mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
                    setAlarms(zmanimCalendar, jDateInfo);
                }
            });
            thread.start();
        }
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(mLocationResolver.getRealtimeNotificationData(this, false));// we will continue in the accept method
    }

    /**
     * This method will set all the alarms/notifications for the upcoming zmanim. This method is
     * called everytime the app starts and everyday by the {@link DailyNotifications} class. If
     * there are zmanim already set to be notified, it removes them all and reschedules them.
     */
    private void setAlarms(ROZmanimCalendar c, JewishDateInfo jDateInfo) {
        Calendar calendar = c.getCalendar();

        calendar.add(Calendar.DATE, -1);
        c.setCalendar(calendar);
        jDateInfo.setCalendar(calendar);//set the calendars to the previous day

        ArrayList<ZmanListEntry> zmanimOver3Days = new ArrayList<>();
        addZmanim(zmanimOver3Days, false, mSettingsPreferences, mSharedPreferences, c, jDateInfo, false);

        calendar.add(Calendar.DATE, 1);
        c.setCalendar(calendar);
        jDateInfo.setCalendar(calendar);//set the calendars to the current day
        addZmanim(zmanimOver3Days, false, mSettingsPreferences, mSharedPreferences, c, jDateInfo, false);

        calendar.add(Calendar.DATE, 1);
        c.setCalendar(calendar);
        jDateInfo.setCalendar(calendar);//set the calendars to the next day
        addZmanim(zmanimOver3Days, false, mSettingsPreferences, mSharedPreferences, c, jDateInfo, false);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        int max = 5;
        for (int i = 0; i < max; i++) {// since we only set a max of 5 zmanim every half an hour, we only need to cancel 5 intents every time
            PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(
                    context.getApplicationContext(),
                    i,
                    new Intent(context, ZmanNotification.class).setAction(String.valueOf(i)),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            am.cancel(zmanPendingIntent);
        }

        int set = 0;// only set 5 zmanim an hour
        for (int i = 0; i < zmanimOver3Days.size(); i++) {
            if (set < max) {
                if (zmanimOver3Days.get(i).getZman() != null &&
                        (zmanimOver3Days.get(i).getZman().getTime() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay(mSettingsPreferences)) > new Date().getTime())) {
                    PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(
                            context.getApplicationContext(),
                            set,
                            new Intent(context, ZmanNotification.class)
                                    .setAction(String.valueOf(set))
                                    .putExtra("zman",
                                            zmanimOver3Days.get(i).getTitle() + ":" + zmanimOver3Days.get(i).getZman().getTime()) //save the zman name and time for the notification e.g. "Chatzot Layla:1331313311"
                                    .putExtra("secondsTreatment", zmanimOver3Days.get(i).getSecondTreatment().getValue()),
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

                    if (zmanimOver3Days.get(i).getNotificationDelay(mSettingsPreferences) != -1) {// only notify if the user wants to be notified i.e. anything greater than -1
                        setExactAndAllowWhileIdle(am, zmanimOver3Days.get(i).getZman().getTime() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay(mSettingsPreferences)), zmanPendingIntent);
                        set++;
                    }
                }
            }
        }
        PendingIntent schedulePendingIntent = PendingIntent.getBroadcast(
                context.getApplicationContext(),
                0,
                new Intent(context, ZmanimNotifications.class),
                PendingIntent.FLAG_IMMUTABLE);

        setExactAndAllowWhileIdle(am, new Date().getTime() + 1_800_000, schedulePendingIntent);//every half hour
    }

    @Override
    public void accept(Location location) {
        if (location != null) {
            String locationName = mLocationResolver.getLocationAsName(location.getLatitude(), location.getLongitude());
            mLocationResolver.resolveElevation(() -> {
                ROZmanimCalendar zmanimCalendar = new ROZmanimCalendar(new GeoLocation(
                        locationName,
                        location.getLatitude(),
                        location.getLongitude(),
                        mLocationResolver.getElevation(),
                        mLocationResolver.getTimeZone()));
                zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
                String candles = mSettingsPreferences.getString("CandleLightingOffset", "20");
                if (candles.isEmpty()) {
                    candles = "20";
                }
                zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
                String shabbat = mSettingsPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
                if (shabbat.isEmpty()) {// for some reason this is happening
                    shabbat = "40";
                }
                zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
                if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                    zmanimCalendar.setAteretTorahSunsetOffset(30);
                }
                mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
                setAlarms(zmanimCalendar, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)));
            });
        } else {
            ROZmanimCalendar zmanimCalendar = new ROZmanimCalendar(mLocationResolver.getLastKnownGeoLocation());
            zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));
            String candles = mSettingsPreferences.getString("CandleLightingOffset", "20");
            if (candles.isEmpty()) {
                candles = "20";
            }
            zmanimCalendar.setCandleLightingOffset(Double.parseDouble(candles));
            String shabbat = mSettingsPreferences.getString("EndOfShabbatOffset", mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40");
            if (shabbat.isEmpty()) {// for some reason this is happening
                shabbat = "40";
            }
            zmanimCalendar.setAteretTorahSunsetOffset(Double.parseDouble(shabbat));
            if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                zmanimCalendar.setAteretTorahSunsetOffset(30);
            }
            mSharedPreferences.edit().putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName()).apply();
            setAlarms(zmanimCalendar, new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false)));
        }
    }
}
