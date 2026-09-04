package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)
                || Objects.equals(intent.getAction(), Intent.ACTION_MY_PACKAGE_REPLACED)) {
            context.sendBroadcast(new Intent(context, ZmanimNotifications.class));
        }
    }
}
