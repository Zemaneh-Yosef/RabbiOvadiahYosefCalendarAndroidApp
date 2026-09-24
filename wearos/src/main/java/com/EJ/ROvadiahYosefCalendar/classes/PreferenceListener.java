package com.EJ.ROvadiahYosefCalendar.classes;

import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class PreferenceListener extends WearableListenerService {

    /**
     * Called by the Wear OS runtime when a message arrives.  We persist the raw
     * payload to SharedPreferences; MainActivity reads it on its next launch via
     * the normal startup path — no data is lost.  While MainActivity is running it
     * also registers its own MessageClient listener and updates itself right away.
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);
        if ("prefs/".equals(messageEvent.getPath())) {
            prefs.edit()
                    .putString("pendingPrefsJson", message)
                    .apply();
        } else if ("chaiTable/".equals(messageEvent.getPath())) {
            String locationName = prefs.getString("locationName", "");
            try {
                locationName = new JSONObject(prefs.getString("pendingPrefsJson", "{}")).optString("locationName", locationName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            prefs.edit()
                    .putString("chaiTable" + Utils.removePostalCode(locationName), message)
                    .apply();
        }
    }
}

