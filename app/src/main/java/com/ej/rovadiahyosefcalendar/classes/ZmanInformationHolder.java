package com.ej.rovadiahyosefcalendar.classes;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.Date;

public class ZmanInformationHolder {

    private Pair<String, Date> nameDatePair;
    private int notificationDelay;

    public ZmanInformationHolder(Pair<String, Date> nameDatePair, int notificationDelay) {
        this.nameDatePair = nameDatePair;
        this.notificationDelay = notificationDelay;
    }

    public Pair<String, Date> getNameDatePair() {
        return nameDatePair;
    }

    public int getNotificationDelay() {
        return notificationDelay;
    }

    @NonNull
    public String toString() {
        return "ZmanInformationHolder{" +
                "nameDatePair=" + nameDatePair +
                ", notificationDelay=" + notificationDelay +
                '}';
    }
}
