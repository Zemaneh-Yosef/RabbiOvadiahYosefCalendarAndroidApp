package com.EJ.ROvadiahYosefCalendar.classes;

import androidx.annotation.NonNull;

import java.util.Date;

public class ZmanInformationHolder {

    private final String zmanName;
    private final Date zmanDate;
    private final int notificationDelay;
    private final String notificationKey;

    public ZmanInformationHolder(String zmanName, Date zmanDate, int notificationDelay, String notificationKey) {
        this.zmanName = zmanName;
        this.zmanDate = zmanDate;
        this.notificationDelay = notificationDelay;
        this.notificationKey = notificationKey;
    }

    public String getZmanName() {
        return zmanName;
    }

    public Date getZmanDate() {
        return zmanDate;
    }

    public int getNotificationDelay() {
        return notificationDelay;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    @NonNull
    public String toString() {
        return "ZmanInformationHolder{" +
                "name=" + zmanName +
                ", date=" + zmanDate +
                ", notificationDelay=" + notificationDelay +
                '}';
    }

}
