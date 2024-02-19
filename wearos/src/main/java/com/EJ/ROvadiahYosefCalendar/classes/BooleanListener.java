package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.ArrayList;
import java.util.List;

public class BooleanListener {
    private static boolean myBoolean;
    private static final List<BooleanChangedListener> listeners = new ArrayList<>();

    public static boolean getMyBoolean() { return myBoolean; }

    public static void setMyBoolean(boolean value) {
        myBoolean = value;

        for (BooleanChangedListener l : listeners) {
            l.OnMyBooleanChanged();
        }
    }

    public static void addMyBooleanListener(BooleanChangedListener l) {
        listeners.add(l);
    }
}
