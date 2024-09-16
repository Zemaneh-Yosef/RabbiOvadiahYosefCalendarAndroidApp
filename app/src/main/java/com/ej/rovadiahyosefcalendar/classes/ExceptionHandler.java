package com.ej.rovadiahyosefcalendar.classes;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.ej.rovadiahyosefcalendar.activities.ShowErrorActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements java.lang.Thread.UncaughtExceptionHandler {
    private final Activity myContext;
    public static boolean isAppFocused;

    public ExceptionHandler(Activity context) {
        myContext = context;
        isAppFocused = true;
    }

    public void uncaughtException(@NonNull Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        String LINE_SEPARATOR = "\n";
        String errorReport =
                "IF YOU ARE A USER OF THIS APP, SEND A SCREENSHOT OF THIS ERROR TO ElyahuJacobi@gmail.com\n\n" +
                "************ CAUSE OF ERROR ************\n\n" +
                stackTrace +
                "\n************ DEVICE INFORMATION ***********\n" +
                "Brand: " +
                Build.BRAND +
                LINE_SEPARATOR +
                "Device: " +
                Build.DEVICE +
                LINE_SEPARATOR +
                "Model: " +
                Build.MODEL +
                LINE_SEPARATOR +
                "Id: " +
                Build.ID +
                LINE_SEPARATOR +
                "Product: " +
                Build.PRODUCT +
                LINE_SEPARATOR +
                "\n************ FIRMWARE ************\n" +
                "SDK: " +
                Build.VERSION.SDK_INT +
                LINE_SEPARATOR +
                "Release: " +
                Build.VERSION.RELEASE +
                LINE_SEPARATOR +
                "Incremental: " +
                Build.VERSION.INCREMENTAL +
                LINE_SEPARATOR;

        Intent intent = new Intent(myContext, ShowErrorActivity.class);
        intent.putExtra("error", errorReport);
        if (isAppFocused) {
            myContext.startActivity(intent);
        }

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

}