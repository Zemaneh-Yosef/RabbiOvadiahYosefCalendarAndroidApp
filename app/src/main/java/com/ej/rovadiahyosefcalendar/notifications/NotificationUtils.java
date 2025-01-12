package com.ej.rovadiahyosefcalendar.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

public class NotificationUtils {

    /**
     * convenience method to schedule notifications
     * @param am an AlarmManager object
     * @param timeInMillis the time the alarm should go off at
     * @param pendingIntent the intent to be broadcast
     */
    public static void setExactAndAllowWhileIdle(AlarmManager am, long timeInMillis, PendingIntent pendingIntent) {
        am.cancel(pendingIntent);//cancel any previous alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        } else {// on lower android version, app will not crash by setting exact alarms
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }
}
