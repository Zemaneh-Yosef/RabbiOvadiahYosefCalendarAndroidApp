package com.ej.rovadiahyosefcalendar;

import android.app.Application;

import com.ej.rovadiahyosefcalendar.classes.DpadSupport;

public class ZemanehYosefApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DpadSupport.install(this);// D-pad focus ring and TV keyboard, in every screen
    }
}
