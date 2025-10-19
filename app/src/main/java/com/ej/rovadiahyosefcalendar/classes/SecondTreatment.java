package com.ej.rovadiahyosefcalendar.classes;

/**
 * This enum is used to determine how a zman should be displayed. If it is ALWAYS_DISPLAY, it will always display the seconds. If it is ROUND_EARLIER,
 * it will round the seconds down to the nearest minute. If it is ROUND_LATER, it will round the seconds up to the nearest minute.
 */
public enum SecondTreatment {
    ALWAYS_DISPLAY(0),
    ROUND_EARLIER(1),
    ROUND_LATER(2),
    ALWAYS_ROUND_LATER(3);

    private final int value;

    SecondTreatment(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
