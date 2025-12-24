package com.ej.rovadiahyosefcalendar.classes;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSettingsPreferences;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSharedPreferences;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.SetupChaiTablesActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

/**
 * This class contains static methods that are used multiple times
 */
public class Utils {

    /**
     * This method checks if the user is near Israel. The exact coordinate are hard to define, that is why I based the coordinates on what was shown
     * in the sefer, "טובה הארץ מאוד מאוד על גבולות ארץ ישראל" by Rav Chaim Yisrael Shteiner.
     * It is based on the opinion of the Maharikash on the Rambam, which is big enough to cover the other opinions except for one that included
     * almost all of Iraq.
     * It seemed like a nice middle ground.
     * @param latitude the latitude to check
     * @param longitude the longitude to check
     * @return if the latitude and longitude are coordinates inside or near Eretz Yisrael (based on halacha)
     */
    public static boolean isInOrNearIsrael(double latitude, double longitude) {
        return sCurrentTimeZoneID.equals("Asia/Jerusalem");//TODO fix this
        //return latitude >= 29.7 && latitude <= 36.1 && longitude >= 31.5 && longitude <= 38.0;
    }

    public static boolean isLocaleHebrew() {
        return Locale.getDefault().getDisplayLanguage(new Locale.Builder().setLanguage("en").setRegion("US").build()).equals("Hebrew");
    }

    public static String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            return new String(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Mainly use this method to remove the postal code from the location name in chaitable classes so the location name is more generic.
     * @param input the location name
     * @return the location name without the postal code
     */
    public static String removePostalCode(String input) {
        if (input == null) return null;
        return input.replaceAll("\\s*\\([^)]*\\)$", "").trim();// Removes ANY trailing " (something)"
    }

    public static String dateFormatPattern(boolean showSeconds) {
        return (Utils.isLocaleHebrew() ? "H" : "h")
            + ":mm"
            + (showSeconds ? ":ss" : "")
            + (Utils.isLocaleHebrew() ? "" : " aa");
    }

    public static String formatZmanTime(Context context, ZmanListEntry zmanListEntry) {
        return formatZmanTime(context, zmanListEntry.getZman(), zmanListEntry.getSecondTreatment());
    }

    public static String formatZmanTime(Context context, Date zman, SecondTreatment secondTreatment) {
        boolean showSeconds = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowSeconds", false);
        DateFormat noSecondDateFormat = new SimpleDateFormat(dateFormatPattern(false), Locale.getDefault());
        DateFormat yesSecondDateFormat = new SimpleDateFormat(dateFormatPattern(true), Locale.getDefault());
        TimeZone timezone = new LocationResolver(context, null).getTimeZone();
        noSecondDateFormat.setTimeZone(timezone);
        yesSecondDateFormat.setTimeZone(timezone);

        String zmanTime;
        if (secondTreatment == SecondTreatment.ALWAYS_DISPLAY || showSeconds) {
            zmanTime = yesSecondDateFormat.format(zman);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(zman);

            Date zmanDate = zman;
            if ((calendar.get(Calendar.SECOND) > 40) || (calendar.get(Calendar.SECOND) > 20 && secondTreatment == SecondTreatment.ROUND_LATER)) {
                zmanDate = Utils.addMinuteToZman(zman);
            }

            zmanTime = noSecondDateFormat.format(zmanDate);
        }
        return zmanTime;
    }

    /**
     * This is a simple convenience method to add a minute to a date object. If the date is not null,
     * it will return the same date with a minute added to it. Otherwise, if the date is null, it will return null.
     * It is important NOT to use the Calendar class because it takes into account the time zones and leap seconds. If you would like to
     * simply add a minute to a date, use this method.
     * @param date the date object to add a minute to
     * @return the given date a minute ahead if not null
     */
    public static Date addMinuteToZman(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime() + 60_000);
    }

    public static void showVisibleSunriseNotification(Context context) {
        NotificationChannel channel = new NotificationChannel("visible_sunrise", "Visible Sunrise Notification", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("This notification will only be sent after the setup process is complete. Otherwise, please use the sunrise dialog.");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.createNotificationChannel(channel);
        int notificationId = Integer.MAX_VALUE; // Unique ID for this notification

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "visible_sunrise")
                .setSmallIcon(R.drawable.common_full_open_on_phone)
                .setContentTitle(context.getString(R.string.setup_visible_sunrise))
                .setContentText(context.getString(R.string.setup_visible_sunrise_now_wanna_try_later_visit_the_sunrise_description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        new Intent(context, SetupChaiTablesActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    /**
     * Returns the current calendar drawable depending on the current day of the month.
     */
    public static int getCurrentCalendarDrawable(SharedPreferences mSettingsPreferences, Calendar calendar) {
        if (!mSettingsPreferences.getBoolean("useDarkCalendarIcon", false)) {
            return getCurrentCalendarDrawableLight(calendar);
        } else {
            return getCurrentCalendarDrawableDark(calendar);
        }
    }

    public static int getCurrentCalendarDrawableLight(Calendar calendar) {
        return switch (calendar.get(Calendar.DATE)) {
            case (1) -> R.drawable.calendar1;
            case (2) -> R.drawable.calendar2;
            case (3) -> R.drawable.calendar3;
            case (4) -> R.drawable.calendar4;
            case (5) -> R.drawable.calendar5;
            case (6) -> R.drawable.calendar6;
            case (7) -> R.drawable.calendar7;
            case (8) -> R.drawable.calendar8;
            case (9) -> R.drawable.calendar9;
            case (10) -> R.drawable.calendar10;
            case (11) -> R.drawable.calendar11;
            case (12) -> R.drawable.calendar12;
            case (13) -> R.drawable.calendar13;
            case (14) -> R.drawable.calendar14;
            case (15) -> R.drawable.calendar15;
            case (16) -> R.drawable.calendar16;
            case (17) -> R.drawable.calendar17;
            case (18) -> R.drawable.calendar18;
            case (19) -> R.drawable.calendar19;
            case (20) -> R.drawable.calendar20;
            case (21) -> R.drawable.calendar21;
            case (22) -> R.drawable.calendar22;
            case (23) -> R.drawable.calendar23;
            case (24) -> R.drawable.calendar24;
            case (25) -> R.drawable.calendar25;
            case (26) -> R.drawable.calendar26;
            case (27) -> R.drawable.calendar27;
            case (28) -> R.drawable.calendar28;
            case (29) -> R.drawable.calendar29;
            case (30) -> R.drawable.calendar30;
            default -> R.drawable.calendar31;
        };
    }

    public static int getCurrentCalendarDrawableDark(Calendar calendar) {
        return switch (calendar.get(Calendar.DATE)) {
            case (1) -> R.drawable.calendar_1_dark;
            case (2) -> R.drawable.calendar_2_dark;
            case (3) -> R.drawable.calendar_3_dark;
            case (4) -> R.drawable.calendar_4_dark;
            case (5) -> R.drawable.calendar_5_dark;
            case (6) -> R.drawable.calendar_6_dark;
            case (7) -> R.drawable.calendar_7_dark;
            case (8) -> R.drawable.calendar_8_dark;
            case (9) -> R.drawable.calendar_9_dark;
            case (10) -> R.drawable.calendar_10_dark;
            case (11) -> R.drawable.calendar_11_dark;
            case (12) -> R.drawable.calendar_12_dark;
            case (13) -> R.drawable.calendar_13_dark;
            case (14) -> R.drawable.calendar_14_dark;
            case (15) -> R.drawable.calendar_15_dark;
            case (16) -> R.drawable.calendar_16_dark;
            case (17) -> R.drawable.calendar_17_dark;
            case (18) -> R.drawable.calendar_18_dark;
            case (19) -> R.drawable.calendar_19_dark;
            case (20) -> R.drawable.calendar_20_dark;
            case (21) -> R.drawable.calendar_21_dark;
            case (22) -> R.drawable.calendar_22_dark;
            case (23) -> R.drawable.calendar_23_dark;
            case (24) -> R.drawable.calendar_24_dark;
            case (25) -> R.drawable.calendar_25_dark;
            case (26) -> R.drawable.calendar_26_dark;
            case (27) -> R.drawable.calendar_27_dark;
            case (28) -> R.drawable.calendar_28_dark;
            case (29) -> R.drawable.calendar_29_dark;
            case (30) -> R.drawable.calendar_30_dark;
            default -> R.drawable.calendar_31_dark;
        };
    }

    public static class PrefToWatchSender {

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
                                        Log.d("From main app", "Message sent to " + node.getDisplayName() + ". Result: " + result + ". message: " + message);
                                    } else {
                                        Exception exception = task.getException();
                                        Log.e("From main app", "Failed to send message to watch: " + exception);
                                    }
                                });

                                StringBuilder chaiTableForThisYear = new StringBuilder();
                                if (sCurrentLocationName != null && sCurrentLocationName.isEmpty()) {
                                    return;
                                }

                                File vsFile = ChaiTablesWebJava.getVisibleSunriseFile(context.getExternalFilesDir(null), sCurrentLocationName, sJewishDateInfo.getJewishCalendar().getJewishYear());
                                if (!vsFile.isFile()) {
                                    return;
                                }

                                List<Long> vSunriseTimes = Collections.emptyList();
                                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(vsFile))) {
                                    vSunriseTimes = (List<Long>) ois.readObject();
                                } catch (IOException | ClassNotFoundException e) {
                                    e.printStackTrace();
                                }

                                for (Long seconds : vSunriseTimes) {
                                    chaiTableForThisYear.append(seconds).append(":");
                                }

                                byte[] chaiTablePayload = chaiTableForThisYear.toString().getBytes(StandardCharsets.UTF_8); // use UTF-8 since each ASCII character will be 1 byte

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

    public static class NumberToHebrew {

        private static final Map<Integer, String> hebrewUnits = new HashMap<>();
        private static final Map<Integer, String> hebrewTens = new HashMap<>();
        private static final Map<Integer, String> hebrewHundreds = new HashMap<>();
        private static final Map<Integer, String> hebrewThousands = new HashMap<>();

        static {
            hebrewUnits.put(1, "אֶחָד");
            hebrewUnits.put(2, "שְׁנַיִם");
            hebrewUnits.put(3, "שְׁלֹשָׁה");
            hebrewUnits.put(4, "אַרְבָּעָה");
            hebrewUnits.put(5, "חֲמִשָּׁה");
            hebrewUnits.put(6, "שִׁשָּׁה");
            hebrewUnits.put(7, "שִׁבְעָה");
            hebrewUnits.put(8, "שְׁמֹנָה");
            hebrewUnits.put(9, "תִּשְׁעָה");

            hebrewTens.put(10, "עָשָׂרָה");
            hebrewTens.put(20, "עֶשְׂרִים");
            hebrewTens.put(30, "שְׁלֹשִׁים");
            hebrewTens.put(40, "אַרְבָּעִים");
            hebrewTens.put(50, "חֲמִשִּׁים");
            hebrewTens.put(60, "שִׁשִּׁים");
            hebrewTens.put(70, "שִׁבְעִים");
            hebrewTens.put(80, "שְׁמֹנִים");
            hebrewTens.put(90, "תִּשְׁעִים");

            hebrewHundreds.put(100, "מֵאָה");
            hebrewHundreds.put(200, "מָאתַיִם");
            hebrewHundreds.put(300, "שְׁלֹשׁ מֵאוֹת");
            hebrewHundreds.put(400, "אַרְבַּע מֵאוֹת");
            hebrewHundreds.put(500, "חֲמֵשׁ מֵאוֹת");
            hebrewHundreds.put(600, "שֵׁשׁ מֵאוֹת");
            hebrewHundreds.put(700, "שֶׁבַע מֵאוֹת");
            hebrewHundreds.put(800, "שְׁמוֹנֶה מֵאוֹת");
            hebrewHundreds.put(900, "תְּשַׁע מֵאוֹת");

            hebrewThousands.put(1000, "אֶלֶף");
            hebrewThousands.put(2000, "אֲלָפַיִם");
            hebrewThousands.put(3000, "שְׁלֹשֶׁת אֲלָפִים");
            hebrewThousands.put(4000, "אַרְבָּעַת אֲלָפִים");
            hebrewThousands.put(5000, "חֲמֵשֶׁת אֲלָפִים");
            hebrewThousands.put(6000, "שֵׁשֶׁת אֲלָפִים");
            hebrewThousands.put(7000, "שִׁבְעַת אֲלָפִים");
            hebrewThousands.put(8000, "שְׁמוֹנַת אֲלָפִים");
            hebrewThousands.put(9000, "תִּשְׁעַת אֲלָפִים");
        }

        public static String numberToHebrew(int number) {
            StringBuilder hebrewSentence = new StringBuilder();

            if (number <= 0 || number > 9999) {
                return "מספר לא ידוע"; // Unknown number
            }

            if (number >= 1000) {
                int thousands = (number / 1000) * 1000;
                hebrewSentence.append(hebrewThousands.get(thousands));
                number %= 1000;
                if (number > 0) {
                    hebrewSentence.append(" ו");
                }
            }

            if (number >= 100) {
                int hundreds = (number / 100) * 100;
                hebrewSentence.append(hebrewHundreds.get(hundreds));
                number %= 100;
                if (number > 0) {
                    hebrewSentence.append(" ו");
                }
            }

            if (number >= 20) {
                int tens = (number / 10) * 10;
                hebrewSentence.append(hebrewTens.get(tens));
                number %= 10;
                if (number > 0) {
                    hebrewSentence.append(" ו");
                }
            } else if (number >= 10) {
                hebrewSentence.append(hebrewUnits.get(number - 10)).append(" ").append("עשרה");
                number = 0;
            }

            if (number > 0) {
                hebrewSentence.append(hebrewUnits.get(number));
            }

            return hebrewSentence.toString();
        }
    }
}
