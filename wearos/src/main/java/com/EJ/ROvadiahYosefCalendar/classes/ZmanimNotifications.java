package com.EJ.ROvadiahYosefCalendar.classes;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.EJ.ROvadiahYosefCalendar.classes.Utils.setExactAndAllowWhileIdle;
import static com.EJ.ROvadiahYosefCalendar.classes.ZmanimFactory.addZmanim;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.kosherjava.zmanim.util.GeoLocation;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZmanimNotifications extends BroadcastReceiver {

    private SharedPreferences mSharedPreferences;
    private Context context;
    private PendingResult mPendingResult;
    private final AtomicBoolean mFinished = new AtomicBoolean();
    private final AtomicBoolean mGotLocation = new AtomicBoolean();
    private boolean mWaitingForLocation;

    @Override
    public void onReceive(Context context, Intent intent) {
        mPendingResult = goAsync();

        new Thread(() -> {
            try {
                this.context = context;
                mSharedPreferences = context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

                if (mSharedPreferences.getBoolean("isSetup", false) &&
                        mSharedPreferences.getBoolean("zmanim_notifications", true)) {

                    ROZmanimCalendar zmanimCalendar = getROZmanimCalendar();

                    if (!zmanimCalendar.getGeoLocation().equals(new GeoLocation())) {// if equal, we will get the user's location in the accept method
                        JewishDateInfo jDateInfo = new JewishDateInfo(mSharedPreferences.getBoolean("inIsrael", false));

                        zmanimCalendar.setExternalFilesDir(context.getExternalFilesDir(null));

                        String candles = mSharedPreferences.getString("CandleLightingOffset", "20");

                        zmanimCalendar.setCandleLightingOffset(Utils.parseDoubleOrDefault(candles, 20));

                        String shabbat = mSharedPreferences.getString(
                                "EndOfShabbatOffset",
                                mSharedPreferences.getBoolean("inIsrael", false) ? "30" : "40"
                        );

                        zmanimCalendar.setAteretTorahSunsetOffset(Utils.parseDoubleOrDefault(shabbat, mSharedPreferences.getBoolean("inIsrael", false) ? 30 : 40));

                        if (mSharedPreferences.getBoolean("inIsrael", false) && shabbat.equals("40")) {
                            zmanimCalendar.setAteretTorahSunsetOffset(30);
                        }

                        mSharedPreferences.edit()
                                .putString("locationNameFN", zmanimCalendar.getGeoLocation().getLocationName())
                                .apply();

                        setAlarms(zmanimCalendar, jDateInfo);
                    } else {
                        mWaitingForLocation = true;
                    }
                }

            } finally {
                if (!mWaitingForLocation) {
                    finishBroadcast();
                }
            }
        }).start();
    }

    private void finishBroadcast() {
        if (mFinished.compareAndSet(false, true)) {
            mPendingResult.finish(); // REQUIRED TO AVOID ANR
        }
    }

    private ROZmanimCalendar getROZmanimCalendar() {
        return new ROZmanimCalendar(new GeoLocation(
                mSharedPreferences.getString("currentLN", ""),
                Double.parseDouble(mSharedPreferences.getString("currentLat", "0")),
                Double.parseDouble(mSharedPreferences.getString("currentLong", "0")),
                getLastKnownElevation(),
                ZoneId.of(mSharedPreferences.getString("currentTimezone", ZoneId.systemDefault().getId()))), mSharedPreferences);
    }

    private double getLastKnownElevation() {
        double elevation;
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            elevation = 0;
        } else {
            elevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("currentLN", ""), "0"));//lastKnownLocation
        }
        return Math.max(elevation, 0);//GeoLocation.setElevation rejects negative values
    }

    private void setAlarms(ROZmanimCalendar c, JewishDateInfo jDateInfo) {
        jDateInfo.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate().minusDays(1));
        c.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate());// set the calendars to the previous day
        ArrayList<ZmanListEntry> zmanimOver3Days = new ArrayList<>();
        addZmanim(zmanimOver3Days, false, mSharedPreferences, c, jDateInfo, false);

        jDateInfo.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate().plusDays(1));
        c.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate());// set the calendars to the current day
        addZmanim(zmanimOver3Days, false, mSharedPreferences, c, jDateInfo, false);

        jDateInfo.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate().plusDays(1));
        c.setLocalDate(jDateInfo.getJewishCalendar().getLocalDate());// set the calendars to the next day
        addZmanim(zmanimOver3Days, false, mSharedPreferences, c, jDateInfo, false);

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
                        (zmanimOver3Days.get(i).getZman().toEpochMilli() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay(mSharedPreferences)) > new Date().getTime()) &&
                        !zmanimOver3Days.get(i).getNotificationKey().isEmpty()) {
                    PendingIntent zmanPendingIntent = PendingIntent.getBroadcast(
                            context.getApplicationContext(),
                            set,
                            new Intent(context, ZmanNotification.class)
                                    .setAction(String.valueOf(set))
                                    .putExtra("zman",
                                            zmanimOver3Days.get(i).getTitle() + ":" + zmanimOver3Days.get(i).getZman().toEpochMilli()) //save the zman name and time for the notification e.g. "Chatzot Layla:1331313311"
                                    .putExtra("zmanKey", zmanimOver3Days.get(i).getNotificationKey())
                                    .putExtra("secondsTreatment", zmanimOver3Days.get(i).getSecondTreatment().getValue()),
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

                    if (zmanimOver3Days.get(i).getNotificationDelay(mSharedPreferences) != -1) {// only notify if the user wants to be notified i.e. anything greater than -1
                        setExactAndAllowWhileIdle(am, zmanimOver3Days.get(i).getZman().toEpochMilli() - (60_000L * zmanimOver3Days.get(i).getNotificationDelay(mSharedPreferences)), zmanPendingIntent);
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
}
