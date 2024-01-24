package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.Date;

public class ZmanListEntry {

    private final String title;
    private final Date zman;
    private final boolean isZman;
    private boolean isNoteworthyZman;
    private boolean isRTZman;
    private boolean shouldBeDimmed;
    private boolean isVisibleSunriseZman;

    public ZmanListEntry(String title) {
        this.title = title;
        this.zman = null;
        this.isZman = false;
    }

    public ZmanListEntry(String title, Date zman, boolean isZman) {
        this.title = title;
        this.zman = zman;
        this.isZman = isZman;
    }

    public ZmanListEntry(String title, Date zman, boolean isZman, boolean isVisibleSunriseZman) {
        this.title = title;
        this.zman = zman;
        this.isZman = isZman;
        this.isVisibleSunriseZman = isVisibleSunriseZman;
    }

    public String getTitle() {
        return title;
    }

    public Date getZman() {
        return zman;
    }

    public boolean isZman() {
        return isZman;
    }

    public void setRTZman(boolean b) {
        isRTZman = b;
    }

    public boolean isRTZman() {
        return isRTZman;
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

    public boolean isVisibleSunriseZman() {
        return isVisibleSunriseZman;
    }
}
