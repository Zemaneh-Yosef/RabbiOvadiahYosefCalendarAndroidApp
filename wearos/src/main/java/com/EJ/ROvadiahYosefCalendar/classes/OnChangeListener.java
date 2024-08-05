package com.EJ.ROvadiahYosefCalendar.classes;

import java.util.ArrayList;
import java.util.List;

public class OnChangeListener {
    private static final List<ChangedListener> listeners = new ArrayList<>();

    public static void notifyListeners() {
        List<ChangedListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }

        for (ChangedListener l : listenersCopy) {
            l.OnChanged();
        }
    }

    public static void addListener(ChangedListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    public static void removeAllListeners() {
        synchronized (listeners) {
            listeners.clear();
        }
    }
}
