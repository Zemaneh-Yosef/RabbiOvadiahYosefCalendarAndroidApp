package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSharedPreferences;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PrefToWatchSender {

    public static void send(Context context) {
        sendPreferencesToWatch(context);
    }

    private static void sendPreferencesToWatch(Context context) {
        WearableCapabilityChecker wearableCapabilityChecker = new WearableCapabilityChecker(context);
        wearableCapabilityChecker.checkIfWatchExists(hasWatch -> {
            if (hasWatch) {
                new Thread(() -> {
                    // Get the connected nodes (user may have multiple watches) on the Wear network
                    Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
                    try {
                        List<Node> nodes = Tasks.await(nodeListTask);
                        for (Node node : nodes) {
                            // Build the message
                            JSONObject jsonPreferences = getJSONPreferencesObject();
                            String message = jsonPreferences.toString();
                            byte[] payload = message.getBytes(StandardCharsets.UTF_8); // use UTF-8 since each ASCII character will be 1 byte

                            // Send the message
                            Task<Integer> sendMessageTask =
                                    Wearable.getMessageClient(context)
                                            .sendMessage(node.getId(), "prefs/", payload);

                            // Add onCompleteListener to check if the message was successfully sent
                            sendMessageTask.addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    int result = task.getResult();
                                    Log.d("From main app", "Message sent to " + node.getDisplayName() + ". Result: " + result + ". message: " +message);
                                } else {
                                    Exception exception = task.getException();
                                    Log.e("From main app", "Failed to send message to watch: " + exception);
                                }
                            });

                            try {
                                ChaiTables chaiTables = new ChaiTables(context.getExternalFilesDir(null), sCurrentLocationName, mJewishDateInfo.getJewishCalendar());
                                String chaiTableForThisYear = chaiTables.getFullChaiTable();
                                byte[] chaiTablePayload = chaiTableForThisYear.getBytes(StandardCharsets.UTF_8); // use UTF-8 since each ASCII character will be 1 byte

                                Task<Integer> sendChaiTablesTask =
                                        Wearable.getMessageClient(context)
                                                .sendMessage(node.getId(), "chaiTable/", chaiTablePayload);

                                sendChaiTablesTask.addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        int result = task.getResult();
                                        Log.d("From main app", "chaiTable sent to " + node.getDisplayName() + ". Result: " + result + ". message: " + message);
                                    } else {
                                        Exception exception = task.getException();
                                        Log.e("From main app", "Failed to send chaiTable to watch: " + exception);
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("From main app", "Failed to build ChaiTables object, file is probably missing");
                                e.printStackTrace();
                            }
                        }
                    } catch (ExecutionException | InterruptedException | JSONException exception) {
                        Log.e("From main app", "Failed to send message to watch: " + exception);
                    }
                }).start();
            }
        });
    }

    private static JSONObject getJSONPreferencesObject() throws JSONException {
        // We need to be careful to remember where the preferences are in either Settings Preferences or Shared Preferences
        JSONObject jsonObject = new JSONObject().put("useElevation", sSharedPreferences.getBoolean("useElevation", false))
                .put("ShowSeconds", sSettingsPreferences.getBoolean("ShowSeconds", false))
                .put("ShowElevation", sSettingsPreferences.getBoolean("ShowElevation", false))
                .put("ShowElevatedSunrise", sSettingsPreferences.getBoolean("ShowElevatedSunrise", false))
                .put("inIsrael", sSharedPreferences.getBoolean("inIsrael", false))
                .put("tekufaOpinions", sSettingsPreferences.getString("tekufaOpinions", "1"))
                .put("RoundUpRT", sSettingsPreferences.getBoolean("RoundUpRT", false))
                .put("showShabbatMevarchim", sSettingsPreferences.getBoolean("showShabbatMevarchim", false))
                .put("LuachAmudeiHoraah", sSettingsPreferences.getBoolean("LuachAmudeiHoraah", false))
                .put("isZmanimInHebrew", sSharedPreferences.getBoolean("isZmanimInHebrew", false))
                .put("isZmanimEnglishTranslated", sSharedPreferences.getBoolean("isZmanimEnglishTranslated", false))
                .put("ShowMishorAlways", sSettingsPreferences.getBoolean("ShowMishorAlways", false))
                .put("plagOpinion", sSettingsPreferences.getString("plagOpinion", "1"))
                .put("CandleLightingOffset", sSettingsPreferences.getString("CandleLightingOffset", "20"))
                .put("ShowWhenShabbatChagEnds", sSettingsPreferences.getBoolean("ShowWhenShabbatChagEnds", false));
        if (jsonObject.getBoolean("ShowWhenShabbatChagEnds")) {
            Set<String> stringSet = sSettingsPreferences.getStringSet("displayRTOrShabbatRegTime", null);
            if (stringSet != null) {
                jsonObject.put("Show Regular Minutes", stringSet.contains("Show Regular Minutes"))
                        .put("Show Rabbeinu Tam", stringSet.contains("Show Rabbeinu Tam"));
            }
        }
        jsonObject.put("EndOfShabbatOffset", sSettingsPreferences.getString("EndOfShabbatOffset", "40"))
                .put("EndOfShabbatOpinion", sSettingsPreferences.getString("EndOfShabbatOpinion", "1"))
                .put("alwaysShowTzeitLChumra", sSettingsPreferences.getBoolean("alwaysShowTzeitLChumra", false))
                .put("AlwaysShowRT", sSettingsPreferences.getBoolean("AlwaysShowRT", false))
                .put("useZipcode", sSharedPreferences.getBoolean("useZipcode", false))
                .put("Zipcode", sSharedPreferences.getString("Zipcode", ""))
                .put("oldZipcode", sSharedPreferences.getString("oldZipcode", "None"))
                .put("oldLocationName", sSharedPreferences.getString("oldLocationName", ""))
                .put("oldLat", sSharedPreferences.getLong("oldLat", 0))
                .put("oldLong", sSharedPreferences.getLong("oldLong", 0))
                .put("locationName", sCurrentLocationName) // needed because we are not sure if the watches current location is the same as the app's
                .put("elevation" + sCurrentLocationName, sSharedPreferences.getString("elevation" + sCurrentLocationName, "0"))
                .put("SetElevationToLastKnownLocation", sSettingsPreferences.getBoolean("SetElevationToLastKnownLocation", false))

                .put("currentLN", sCurrentLocationName)
                .put("currentLat", String.valueOf(sLatitude))
                .put("currentLong", String.valueOf(sLongitude))
                .put("currentTimezone", sCurrentTimeZoneID)

                .put("useAdvanced", sSharedPreferences.getBoolean("useAdvanced", false))
                .put("advancedLN", sSharedPreferences.getString("advancedLN", ""))
                .put("advancedLat", sSharedPreferences.getString("advancedLat", "0"))
                .put("advancedLong", sSharedPreferences.getString("advancedLong", "0"))
                .put("advancedTimezone", sSharedPreferences.getString("advancedTimezone", ""))

                .put("useLocation1", sSharedPreferences.getBoolean("useLocation1", false))
                .put("location1", sSharedPreferences.getString("location1", ""))
                .put("location1Lat", sSharedPreferences.getLong("location1Lat", 0))
                .put("location1Long", sSharedPreferences.getLong("location1Long", 0))
                .put("location1Timezone", sSharedPreferences.getString("location1Timezone", ""))

                .put("useLocation2", sSharedPreferences.getBoolean("useLocation2", false))
                .put("location2", sSharedPreferences.getString("location2", ""))
                .put("location2Lat", sSharedPreferences.getLong("location2Lat", 0))
                .put("location2Long", sSharedPreferences.getLong("location2Long", 0))
                .put("location2Timezone", sSharedPreferences.getString("location2Timezone", ""))

                .put("useLocation3", sSharedPreferences.getBoolean("useLocation3", false))
                .put("location3", sSharedPreferences.getString("location3", ""))
                .put("location3Lat", sSharedPreferences.getLong("location3Lat", 0))
                .put("location3Long", sSharedPreferences.getLong("location3Long", 0))
                .put("location3Timezone", sSharedPreferences.getString("location3Timezone", ""))

                .put("useLocation4", sSharedPreferences.getBoolean("useLocation4", false))
                .put("location4", sSharedPreferences.getString("location4", ""))
                .put("location4Lat", sSharedPreferences.getLong("location4Lat", 0))
                .put("location4Long", sSharedPreferences.getLong("location4Long", 0))
                .put("location4Timezone", sSharedPreferences.getString("location4Timezone", ""))

                .put("useLocation5", sSharedPreferences.getBoolean("useLocation5", false))
                .put("location5", sSharedPreferences.getString("location5", ""))
                .put("location5Lat", sSharedPreferences.getLong("location5Lat", 0))
                .put("location5Long", sSharedPreferences.getLong("location5Long", 0))
                .put("location5Timezone", sSharedPreferences.getString("location5Timezone", ""))

                .put("zmanim_notifications", sSettingsPreferences.getBoolean("zmanim_notifications", false))
                .put("NightChatzot", sSettingsPreferences.getInt("NightChatzot", -1))
                .put("RT", sSettingsPreferences.getInt("RT", -1))
                .put("ShabbatEnd", sSettingsPreferences.getInt("ShabbatEnd", -1))
                .put("FastEnd", sSettingsPreferences.getInt("FastEnd", -1))
                .put("TzeitHacochavimLChumra", sSettingsPreferences.getInt("TzeitHacochavimLChumra", -1))
                .put("TzeitHacochavim", sSettingsPreferences.getInt("TzeitHacochavim", -1))
                .put("Shkia", sSettingsPreferences.getInt("Shkia", -1))
                .put("CandleLighting", sSettingsPreferences.getInt("CandleLighting", -1))
                .put("PlagHaMinchaYY", sSettingsPreferences.getInt("PlagHaMinchaYY", -1))
                .put("PlagHaMinchaHB", sSettingsPreferences.getInt("PlagHaMinchaHB", -1))
                .put("MinchaKetana", sSettingsPreferences.getInt("MinchaKetana", -1))
                .put("MinchaGedola", sSettingsPreferences.getInt("MinchaGedola", -1))
                .put("Chatzot", sSettingsPreferences.getInt("Chatzot", -1))
                .put("SofZmanBiurChametz", sSettingsPreferences.getInt("SofZmanBiurChametz", -1))
                .put("SofZmanTefila", sSettingsPreferences.getInt("SofZmanTefila", -1))
                .put("SofZmanAchilatChametz", sSettingsPreferences.getInt("SofZmanAchilatChametz", -1))
                .put("SofZmanShmaGRA", sSettingsPreferences.getInt("SofZmanShmaGRA", -1))
                .put("SofZmanShmaMGA", sSettingsPreferences.getInt("SofZmanShmaMGA", -1))
                .put("HaNetz", sSettingsPreferences.getInt("HaNetz", -1))
                .put("TalitTefilin", sSettingsPreferences.getInt("TalitTefilin", -1))
                .put("Alot", sSettingsPreferences.getInt("Alot", -1))
                .put("zmanim_notifications_on_shabbat", sSettingsPreferences.getBoolean("zmanim_notifications_on_shabbat", false))
                .put("autoDismissNotifications", sSettingsPreferences.getInt("autoDismissNotifications", -1));

        return jsonObject;
    }
}
