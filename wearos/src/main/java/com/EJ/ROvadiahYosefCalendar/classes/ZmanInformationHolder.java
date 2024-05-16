package com.EJ.ROvadiahYosefCalendar.classes;

import androidx.annotation.NonNull;

import java.util.Date;

public class ZmanInformationHolder {

    private final String zmanName;
    private final Date zmanDate;
    private final int notificationDelay;

    public ZmanInformationHolder(String zmanName, Date zmanDate, int notificationDelay) {
        this.zmanName = zmanName;
        this.zmanDate = zmanDate;
        this.notificationDelay = notificationDelay;
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

    @NonNull
    public String toString() {
        return "ZmanInformationHolder{" +
                "name=" + zmanName +
                ", date=" + zmanDate +
                ", notificationDelay=" + notificationDelay +
                '}';
    }

}
