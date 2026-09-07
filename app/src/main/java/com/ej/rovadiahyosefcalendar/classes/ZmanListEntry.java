package com.ej.rovadiahyosefcalendar.classes;

import android.content.SharedPreferences;

import java.util.Date;

public class ZmanListEntry {

    private String title;
    private Date zman;
    private final ZmanListEntryType zmanListEntryType;
    private boolean isNoteworthyZman;
    private boolean shouldBeDimmed;
    private SecondTreatment secondTreatment;
    private String notificationKey;

    public ZmanListEntry(String title) {
        this.title = title;
        this.zman = null;
        this.zmanListEntryType = ZmanListEntryType.UNSPECIFIED;
    }

    public ZmanListEntry(String title, ZmanListEntryType zmanListEntryType) {
        this.title = title;
        this.zman = null;
        this.zmanListEntryType = zmanListEntryType;
    }

    public ZmanListEntry(String title, Date zman, SecondTreatment secondTreatment, ZmanListEntryType zmanListEntryType, String notificationKey) {
        this.title = title;
        this.zman = zman;
        this.zmanListEntryType = zmanListEntryType;
        this.secondTreatment = secondTreatment;
        this.notificationKey = notificationKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getZman() {
        return zman;
    }

    public void setZman(Date zman) {
        this.zman = zman;
    }

    public boolean isZman() {
        return zmanListEntryType.name().toLowerCase().endsWith("zman");
    }

    public ZmanListEntryType getZmanListEntryType() {
        return zmanListEntryType;
    }

    public boolean isNoteworthyZman() {
        return isNoteworthyZman;
    }

    public void setNoteworthyZman(boolean b) {
        isNoteworthyZman = b;
    }

    public boolean isShouldBeDimmed() {
        return shouldBeDimmed;
    }

    public void setShouldBeDimmed(boolean shouldBeDimmed) {
        this.shouldBeDimmed = shouldBeDimmed;
    }

    public SecondTreatment getSecondTreatment() {
        return secondTreatment;
    }

    public int getNotificationDelay(SharedPreferences mSettingsPreferences) {
        return mSettingsPreferences.getInt(notificationKey, -1);
    }
}
