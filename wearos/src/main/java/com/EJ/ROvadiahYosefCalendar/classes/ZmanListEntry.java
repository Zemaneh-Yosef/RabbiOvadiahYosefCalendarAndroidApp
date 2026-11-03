package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.SharedPreferences;

import java.time.Instant;

public class ZmanListEntry {

    private String title;
    private Instant zman;
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

    public ZmanListEntry(String title, Instant zman, SecondTreatment secondTreatment, ZmanListEntryType zmanListEntryType, String notificationKey) {
        this.title = title;
        this.zman = zman;
        this.zmanListEntryType = zmanListEntryType;
        this.secondTreatment = secondTreatment;
        this.notificationKey = notificationKey;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getZman() {
        return this.zman;
    }

    public void setZman(Instant zman) {
        this.zman = zman;
    }

    public boolean isZman() {
        return this.zmanListEntryType.name().toLowerCase().endsWith("zman");
    }

    public ZmanListEntryType getZmanListEntryType() {
        return this.zmanListEntryType;
    }

    public boolean isNoteworthyZman() {
        return this.isNoteworthyZman;
    }

    public void setNoteworthyZman(boolean b) {
        this.isNoteworthyZman = b;
    }

    public boolean isShouldBeDimmed() {
        return this.shouldBeDimmed;
    }

    public void setShouldBeDimmed(boolean shouldBeDimmed) {
        this.shouldBeDimmed = shouldBeDimmed;
    }

    public SecondTreatment getSecondTreatment() {
        return this.secondTreatment;
    }

    public int getNotificationDelay(SharedPreferences mSettingsPreferences) {
        return mSettingsPreferences.getInt(this.notificationKey, -1);
    }

    public String getNotificationKey() {
        return this.notificationKey;
    }

    public void setNotificationKey(String s) {
        this.notificationKey = s;
    }
}
