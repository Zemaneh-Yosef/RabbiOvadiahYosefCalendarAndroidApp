package com.EJ.ROvadiahYosefCalendar.classes;

import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

public class PreferenceListener extends WearableListenerService {

    private MessageClient.OnMessageReceivedListener listener;

    /**
     * Called by MainActivity while the app is in the foreground.
     * Registers a MessageClient listener so messages are delivered in-process.
     * startService() must NOT be called for this service — WearableListenerService
     * is started automatically by the Wear OS runtime; calling startService()
     * manually throws BackgroundServiceStartNotAllowedException on API 31+.
     */
    public void setOnMessageReceivedListener(MessageClient.OnMessageReceivedListener listener, Context context) {
        this.listener = listener;
        Wearable.getMessageClient(context).addListener(listener);
    }

    /**
     * Called by the Wear OS runtime when a message arrives AND the app process
     * is not already running (background / killed).  In that case {@code listener}
     * is null because MainActivity.onCreate() never ran, so we persist the raw
     * payload to SharedPreferences ourselves.  MainActivity will read it on its
     * next launch via the normal startup path — no data is lost.
     * <p>
     * When the app IS running, {@code listener} is non-null and the in-process
     * path handles everything (including calling updateAppContents()), so we
     * deliberately skip the SharedPreferences write to avoid processing the
     * message twice.
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (listener != null) {
            // App is in the foreground — delegate to MainActivity's handler.
            listener.onMessageReceived(messageEvent);
        } else {
            // App process is not running — persist the raw data so MainActivity
            // picks it up on next launch.
            SharedPreferences prefs = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
            String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);
            if ("prefs/".equals(messageEvent.getPath())) {
                prefs.edit()
                        .putString("pendingPrefsJson", message)
                        .putBoolean("hasGottenDataFromApp", true)
                        .apply();
            } else if ("chaiTable/".equals(messageEvent.getPath())) {
                // The location name will have been saved by the prefs message
                // which always arrives first; use it as the key, same as MainActivity.
                String locationName = Utils.removePostalCode(prefs.getString("locationName", ""));
                prefs.edit()
                        .putString("chaiTable" + locationName, message)
                        .apply();
            }
        }
    }
}

