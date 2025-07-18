package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.Date;

public class ZmanListEntry {

    private String title;
    private Date zman;
    private final boolean isZman;
    private boolean isNoteworthyZman;
    private boolean shouldBeDimmed;
    private SecondTreatment secondTreatment;
    private boolean isBirchatHachamahZman;
    private boolean is66MisheyakirZman;

    public ZmanListEntry(String title) {
        this.title = title;
        this.zman = null;
        this.isZman = false;
    }

    public ZmanListEntry(String title, Date zman, SecondTreatment secondTreatment) {
        this.title = title;
        this.zman = zman;
        this.isZman = true;
        this.secondTreatment = secondTreatment;
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
        return isZman;
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

    public void setBirchatHachamahZman(boolean birchatHachamahZman) {
        isBirchatHachamahZman = birchatHachamahZman;
    }

    public boolean isBirchatHachamahZman() {
        return isBirchatHachamahZman;
    }

    public boolean is66MisheyakirZman() {
        return is66MisheyakirZman;
    }

    public void setIs66MisheyakirZman(boolean is66MisheyakirZman) {
        this.is66MisheyakirZman = is66MisheyakirZman;
    }
}
