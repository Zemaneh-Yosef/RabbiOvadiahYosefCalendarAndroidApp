package com.EJ.ROvadiahYosefCalendar.classes;

public enum SecondTreatment {
    ALWAYS_DISPLAY(0),
    ROUND_EARLIER(1),
    ROUND_LATER(2);

    private final int value;

    SecondTreatment(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
