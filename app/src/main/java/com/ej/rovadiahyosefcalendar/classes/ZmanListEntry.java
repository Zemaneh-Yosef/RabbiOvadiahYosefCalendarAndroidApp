package com.ej.rovadiahyosefcalendar.classes;

import java.util.Date;

public class ZmanListEntry {

    private final String title;
    private final Date zman;
    private final boolean isZman;
    private boolean isNoteworthyZman;
    private boolean isRTZman;

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
}
