package com.ej.rovadiahyosefcalendar.classes;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;
import com.kosherjava.zmanim.util.GeoLocation;

import net.iakovlev.timeshape.TimeZoneEngine;

import org.geonames.WebService;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class LocationResolver {

    public static TimeZoneEngine ENGINE = null;
    private static boolean sTimeZoneEngineHasBeenInitialized = false;
    private final Context mContext;
    private final Activity mActivity;
    private final Geocoder mGeocoder;
    private final SharedPreferences mSharedPreferences;
    private String mLocationName;
    private double mLatitude;
    private double mLongitude;
    private double mElevation;
    private TimeZone mTimeZone = TimeZone.getDefault();

    public LocationResolver(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
        mGeocoder = new Geocoder(mContext, Locale.getDefault());
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
    }

    /**
     * This method gets the device's latitude and longitude. It will ask for permission
     * if we do not have it, and it will alert the user if location services is disabled.
     * <p>
     * As of Android 11 (API 30) there is a more accurate way of getting the current location of the
     * device, however, the process is slower as it needs to actually make a call to the GPS service
     * if the location has not been updated recently.
     * <p>
     * I originally wanted to just allow users to use a zip code to find their location, but I noticed that it also takes into account any address.
     * The old keys for shared preferences are still there as zipcodes because I did not want to change them and undo the work the users have done.
     */
    public void acquireLatitudeAndLongitude(Consumer<Location> consumer) {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            sCurrentLocationName = mSharedPreferences.getString("advancedLN", "");
            try {
                sLatitude = Double.parseDouble(mSharedPreferences.getString("advancedLat", "0.0"));
                sLongitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", "0.0"));
            } catch (NumberFormatException e) {
                sLatitude = 0.0;
                sLongitude = 0.0;
            }
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location1", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Long", 0));
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location2", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Long", 0));
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location3", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Long", 0));
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location4", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Long", 0));
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location5", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Long", 0));
        } else if (mSharedPreferences.getBoolean("useZipcode", false)) {
            getLatitudeAndLongitudeFromSearchQuery();
        } else {
            if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                if (mActivity != null) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 1);
                }
            } else {
                LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(mContext, mContext.getString(R.string.please_enable_gps), Toast.LENGTH_SHORT).show();
                    }
                    // As of now, we make two requests and the GPS, which is usually more accurate, takes longer to respond. For now, this results in the UI being updated twice with the more accurate GPS location. Maybe only update the UI if the GPS is more accurate?
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Runnable::run, consumer);
                        }
                        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                            locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Runnable::run, consumer);
                        }
                    } else {
                        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                            if (location == null) {
                                if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                            }
                            if (location != null) {
                                sLatitude = location.getLatitude();
                                sLongitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        }
        resolveCurrentLocationName();
    }

    /**
     * Resolves the current location name to be a latitude and longitude if the current location name is empty
     * @see MainFragmentManager#sCurrentLocationName
     */
    public void resolveCurrentLocationName() {
        sCurrentLocationName = getLocationAsName();
        if (sCurrentLocationName.isEmpty()) {
            String lat = String.format(Locale.getDefault(), "%.3f", sLatitude);
            String longitude = String.format(Locale.getDefault(), "%.3f", sLongitude);

            sCurrentLocationName = "Lat: " + lat + ", Long: " + longitude;
        }
        mSharedPreferences.edit().putString("name", sCurrentLocationName).apply();
    }

    public String getFullLocationName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            if (Utils.isLocaleHebrew()) {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {
            if (Utils.isLocaleHebrew()) {
                Address address = null;
                for (Address add : addresses) {
                    if (add.getLocale().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                        address = add;
                    }
                }
                String city = null;
                if (address != null) {
                    city = address.getLocality();
                }
                if (city != null) {
                    result.append(city).append(", ");
                }

                String state = null;
                if (address != null) {
                    state = address.getAdminArea();
                }
                if (state != null) {
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                if (city == null && state == null) {
                    String country = null;
                    if (address != null) {
                        country = address.getCountryName();
                    }
                    result.append(country);
                }
            } else {
                String country = addresses.get(0).getCountryName();

                String featureName = addresses.get(0).getFeatureName();
                if (featureName != null
                        && !featureName.matches("^[0-9-]*$")
                        && !addresses.get(0).getAddressLine(0).startsWith(featureName)
                        && !featureName.equals(country)) {
                    result.append(featureName).append(", ");
                }

                String city = addresses.get(0).getLocality();
                if (city != null && (featureName == null || !featureName.equals(city))) {
                    result.append(city).append(", ");
                }

                String county = addresses.get(0).getSubAdminArea();
                if (county != null && (city == null || !county.contains(city))) {
                    // County city check made for Los Angeles, that has a county of "Los Angeles County"
                    result.append(county).append(", ");
                }

                String state = addresses.get(0).getAdminArea();
                if (state != null && (city == null || !state.contains(city)) && (!state.equals(county))) {
                    // State city check made for Jerusalem, that has a county of "Jerusalem"
                    // County equals check to account Rio De Janeiro
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 1);
                    result.deleteCharAt(result.length() - 1);
                }

                if ((city == null && state == null) || (featureName != null && featureName.equals(addresses.get(0).getCountryName()))) {
                    if (featureName != null && featureName.equals(addresses.get(0).getCountryName())) {
                        result.append(", ");
                    }
                    result.append(country);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                String postalCode = addresses.get(0).getPostalCode();
                if (postalCode != null) {
                    result.append(" (").append(postalCode).append(")");
                }
            }
        }
        return result.toString().trim();
    }

    /**
     * This method uses the Geocoder class to try and get the current location's name. I have
     * tried to make my results similar to the zmanim app by JGindin on the Play Store. In america,
     * it will get the current location by state and city. Whereas, in other areas of the world, it
     * will get the country and the city.
     *
     * @return a string containing the name of the current city and state/country that the user is located in.
     * @see Geocoder
     */
    public String getLocationAsName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            if (Utils.isLocaleHebrew()) {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {

            if (Utils.isLocaleHebrew()) {
                Address address = null;
                for (Address add : addresses) {
                    if (add.getLocale().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                        address = add;
                    }
                }
                String city = null;
                if (address != null) {
                    city = address.getLocality();
                }
                if (city != null) {
                    result.append(city).append(", ");
                }

                String state = null;
                if (address != null) {
                    state = address.getAdminArea();
                }
                if (state != null) {
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                if (city == null && state == null) {
                    String country = null;
                    if (address != null) {
                        country = address.getCountryName();
                    }
                    result.append(country);
                }
            } else {
                String city = addresses.get(0).getLocality();
                if (city != null) {
                    result.append(city).append(", ");
                }

                String state = addresses.get(0).getAdminArea();
                if (state != null) {
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                if (city == null && state == null) {
                    String country = addresses.get(0).getCountryName();
                    result.append(country);
                }
            }
        }
        return result.toString().trim();
    }

    /**
     * This method uses the Geocoder class to get a latitude and longitude coordinate from the user
     * specified zip code/area.
     *
     * @see Geocoder
     */
    public void getLatitudeAndLongitudeFromSearchQuery() {
        String zipcode = mSharedPreferences.getString("Zipcode", "");
        if (zipcode.equals(mSharedPreferences.getString("oldZipcode", "None"))) {
            getOldSearchLocation();
            return;
        }
        List<Address> address = null;
        try {
            address = mGeocoder.getFromLocationName(zipcode, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((address != null ? address.size() : 0) > 0) {
            Address first = address.get(0);
            sLatitude = first.getLatitude();
            sLongitude = first.getLongitude();
            sCurrentLocationName = getLocationAsName();
            mLocationName = sCurrentLocationName;
            mSharedPreferences.edit()
                    .putString("oldZipcode", zipcode)
                    .putString("oldLocationName", sCurrentLocationName)
                    .putLong("oldLat", Double.doubleToRawLongBits(sLatitude))
                    .putLong("oldLong", Double.doubleToRawLongBits(sLongitude))
                    .apply();
        } else {
            getOldSearchLocation();
        }
    }

    /**
     * This method retrieves the old location data from the device's storage if it has already been
     * setup beforehand.
     *
     * @see #getLatitudeAndLongitudeFromSearchQuery()
     */
    public void getOldSearchLocation() {
        sCurrentLocationName = mSharedPreferences.getString("oldLocationName", "");
        double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
        double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));

        if (oldLat != 0 && oldLong != 0) {
            sLatitude = oldLat;
            sLongitude = oldLong;
        }
    }

    /**
     * This method initializes the TimeZoneEngine if it has not been already and then returns it. Ideally, this method should be called as early as
     * possible in the application's lifecycle in order to avoid any long pauses.
     * @return the TimeZoneEngine object
     */
    public static TimeZoneEngine getTimeshapeEngine() {
        if (ENGINE == null && !sTimeZoneEngineHasBeenInitialized) {
            new Thread(() -> ENGINE = TimeZoneEngine.initialize()).start();
            sTimeZoneEngineHasBeenInitialized = true;
        }
        return ENGINE;
    }

    /**
     * This method will try to find the timezone of the location set. It uses the TimeZoneMap to get the current timezone ID when using a zipcode
     */
    public void setTimeZoneID() {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("advancedTimezone", TimeZone.getDefault().getID()));
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location1Timezone", TimeZone.getDefault().getID()));
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location2Timezone", TimeZone.getDefault().getID()));
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location3Timezone", TimeZone.getDefault().getID()));
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location4Timezone", TimeZone.getDefault().getID()));
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location5Timezone", TimeZone.getDefault().getID()));
        } else {
            if (mSharedPreferences.getBoolean("useZipcode", false)) {
                String savedZipcodeLocation = "location";
                for (int i = 1; i <= 5; i++) {
                    if (mSharedPreferences.getString("location" + i, "").equals(mLocationName)) {
                        savedZipcodeLocation = "location" + i;
                        break;
                    }
                }
                if (mSharedPreferences.getString(savedZipcodeLocation, "").equals(mLocationName)) {
                    mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString(savedZipcodeLocation + "Timezone", TimeZone.getDefault().getID()));
                    sCurrentTimeZoneID = mTimeZone.getID();
                    return; // basically, avoid making the engine if we already got the timezone last time
                }
                try {
                    if (sLatitude != 0.0 && sLongitude != 0.0) {
                        while (ENGINE == null) {// we need to wait for the TimeZoneEngine to be initialized
                            if (!sTimeZoneEngineHasBeenInitialized) {
                                getTimeshapeEngine();
                            }
                        }
                        String zoneID = TimeZone.getDefault().getID();
                        List<ZoneId> allZones = getTimeshapeEngine().queryAll(sLatitude, sLongitude);// first query all possible time zones in the area. There could be multiple due to border disputes
                        if (allZones.size() > 1) {// if there are multiple
                            for (ZoneId zone : allZones) {
                                zoneID = zone.toString();
                                if (zone.toString().equals(TimeZone.getDefault().getID())) {// if the zone is the device default, assumingly use that
                                    break;
                                }
                            }
                        } else if (allZones.size() == 1) {// if there is only one
                            zoneID = allZones.get(0).toString();
                        }
                        mTimeZone = TimeZone.getTimeZone(zoneID);
                        sCurrentTimeZoneID = mTimeZone.getID();// need to set this for the saveLocationInformation method
                        saveLocationInformation();// only want to use this when people search their location
                    }
                } catch (IllegalArgumentException e) {
                    mTimeZone = TimeZone.getDefault();
                }
            } else {
                mTimeZone = TimeZone.getDefault();
            }
        }
        if (mTimeZone.getID().equals("Asia/Gaza") || mTimeZone.getID().equals("Asia/Hebron")) {
            mTimeZone = TimeZone.getTimeZone("Asia/Jerusalem");
        }
        sCurrentTimeZoneID = mTimeZone.getID();
    }

    /**
     * Convenience method to get the timezone of a place when the user selects a place on the map. AKA advancedLocation
     */
    public void acquireTimeZoneID() {
        try {
            while (ENGINE == null) {// we need to wait for the TimeZoneEngine to be initialized
                if (!sTimeZoneEngineHasBeenInitialized) {
                    getTimeshapeEngine();
                }
            }
            String zoneID = TimeZone.getDefault().getID();
            List<ZoneId> allZones = getTimeshapeEngine().queryAll(sLatitude, sLongitude);// first query all possible time zones in the area. There could be multiple due to border disputes
            if (allZones.size() > 1) {// if there are multiple
                for (ZoneId zone : allZones) {
                    zoneID = zone.toString();
                    if (zone.toString().equals(TimeZone.getDefault().getID())) {// if the zone is the device default, assumingly use that
                        break;
                    }
                }
            } else if (allZones.size() == 1) {// if there is only one
                zoneID = allZones.get(0).toString();
            }
            mTimeZone = TimeZone.getTimeZone(zoneID);
        } catch (IllegalArgumentException e) {
            mTimeZone = TimeZone.getDefault();
        }
        if (mTimeZone.getID().equals("Asia/Gaza") || mTimeZone.getID().equals("Asia/Hebron")) {
            mTimeZone = TimeZone.getTimeZone("Asia/Jerusalem");
        }
        sCurrentTimeZoneID = mTimeZone.getID();
        mSharedPreferences.edit().putString("advancedTimezone", sCurrentTimeZoneID).apply();
    }

    private void saveLocationInformation() {
        Set<String> locations = new HashSet<>();
        locations.add(mSharedPreferences.getString("location1", ""));
        locations.add(mSharedPreferences.getString("location2", ""));
        locations.add(mSharedPreferences.getString("location3", ""));
        locations.add(mSharedPreferences.getString("location4", ""));
        locations.add(mSharedPreferences.getString("location5", ""));

        if (!sCurrentLocationName.isEmpty() && !locations.contains(sCurrentLocationName)) { // if the current location is not empty and the location is not in the last 5 locations

            mSharedPreferences.edit().putString("location5", mSharedPreferences.getString("location4", ""))
                    .putLong("location5Lat", mSharedPreferences.getLong("location4Lat", 0))
                    .putLong("location5Long", mSharedPreferences.getLong("location4Long", 0))
                    .putString("location5Timezone", mSharedPreferences.getString("location4Timezone", "")).apply(); // swap location values

            mSharedPreferences.edit().putString("location4", mSharedPreferences.getString("location3", ""))
                    .putLong("location4Lat", mSharedPreferences.getLong("location3Lat", 0))
                    .putLong("location4Long", mSharedPreferences.getLong("location3Long", 0))
                    .putString("location4Timezone", mSharedPreferences.getString("location3Timezone", "")).apply(); // swap location values

            mSharedPreferences.edit().putString("location3", mSharedPreferences.getString("location2", ""))
                    .putLong("location3Lat", mSharedPreferences.getLong("location2Lat", 0))
                    .putLong("location3Long", mSharedPreferences.getLong("location2Long", 0))
                    .putString("location3Timezone", mSharedPreferences.getString("location2Timezone", "")).apply(); // swap location values

            mSharedPreferences.edit().putString("location2", mSharedPreferences.getString("location1", ""))
                    .putLong("location2Lat", mSharedPreferences.getLong("location1Lat", 0))
                    .putLong("location2Long", mSharedPreferences.getLong("location1Long", 0))
                    .putString("location2Timezone", mSharedPreferences.getString("location1Timezone", "")).apply(); // swap location values

            mSharedPreferences.edit().putString("location1", sCurrentLocationName)
                    .putLong("location1Lat", Double.doubleToRawLongBits(sLatitude))
                    .putLong("location1Long", Double.doubleToRawLongBits(sLongitude))
                    .putString("location1Timezone", sCurrentTimeZoneID).apply(); // save the current location last and we will get the elevation later
        }
    }

    /**
     * This method tries to "resolve" the average elevation of the latitude and longitude set. It will try to make a network call to geonames.org
     * three separate times to request all known elevation data available and it will then average the three results. This method must be called on
     * another thread that is NOT the MAIN UI thread, otherwise, a {@link NetworkOnMainThreadException} will occur. Any other code that
     * needs to run in the background as well can be passed in as a {@link Runnable} in the second parameter. Any UI code that needs
     * to run after this method runs can be passed in as a {@link Runnable} object in the third parameter. Note: The code that is executed by the
     * handler class will run on the MAIN UI thread.
     * @param handler a Handler object with the main ui thread as the main looper.
     * @param codeToRunInBackground a Runnable piece of code that needs to be ran after the web call, or null if not needed.
     * @param codeToRunOnMainThread a Runnable piece of code that also needs to be ran after the web call. I believe it can also be null if not desired.
     */
    public void getElevationFromWebService(Handler handler, Runnable codeToRunInBackground, Runnable codeToRunOnMainThread) {
        WebService.setUserName("Elyahu41");
        ArrayList<Integer> elevations = new ArrayList<>();
        int sum = 0;
        try {
            int e1 = WebService.srtm3(sLatitude, sLongitude);
            if (e1 > 0) {
                elevations.add(e1);
            }
            int e2 = WebService.astergdem(sLatitude, sLongitude);
            if (e2 > 0) {
                elevations.add(e2);
            }
            int e3 = WebService.gtopo30(sLatitude, sLongitude);
            if (e3 > 0) {
                elevations.add(e3);
            }

            for (int e : elevations) {
                sum += e;
            }
        } catch (NumberFormatException | IOException ex) {//an error occurred getting the elevation data, try again!
            try {
                WebService.setUserName("graviton57");//another user api key that I found online, only used as a backup
                int e1 = WebService.srtm3(sLatitude, sLongitude);
                if (e1 > 0) {
                    elevations.add(e1);
                }
                int e2 = WebService.astergdem(sLatitude, sLongitude);
                if (e2 > 0) {
                    elevations.add(e2);
                }
                int e3 = WebService.gtopo30(sLatitude, sLongitude);
                if (e3 > 0) {
                    elevations.add(e3);
                }

                for (int e : elevations) {
                    sum += e;
                }
            } catch (NumberFormatException | IOException ex1) {
                ex.printStackTrace();
                ex1.printStackTrace();
            }
        }
        int size = elevations.size();
        if (size == 0) {
            size = 1;//edge case if no elevation data is available
        }
        mSharedPreferences.edit().putString("elevation" + sCurrentLocationName, String.valueOf(sum / size)).apply();

        if (codeToRunInBackground != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(codeToRunInBackground);  // Submit codeToRunInBackground and get a Future
            // Wait for codeToRunInBackground to complete
            executor.execute(() -> {
                try {
                    future.get();  // This will block until codeToRunInBackground finishes

                    // Now post codeToRunOnMainThread to the main thread
                    handler.post(codeToRunOnMainThread);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            // If codeToRunInBackground is null, just run codeToRunOnMainThread on the main thread
            handler.post(codeToRunOnMainThread);
        }
    }

    /**
     * This method will attempt to get the current location of the device as a GeoLocation object. If the user set a location via the advanced
     * location or a zipcode, it will use that instead and it will not request the device's location.
     * <p> 
     * NOTE: As of Android 10, we can only request the device's location if it is being called by a Service, BroadcastReceiver, or some service 
     * that is in the Foreground. All other requests while the app is not in the foreground will silently fail as Android will log a warning and 
     * not request the location, resulting in the consumer method not being called. Therefore, use the isForWidget parameter to determine if the
     * request is being made by a widget or any other UI element that is not in the foreground.
     * @param consumer the block of code to run when the location is retrieved
     * @param isForWidget if the request is being made by a widget or not
     * @return a GeoLocation object containing the current location
     */
    public GeoLocation getRealtimeNotificationData(Consumer<Location> consumer, boolean isForWidget) {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            mLocationName = mSharedPreferences.getString("advancedLN", "");
            mLatitude = Double.parseDouble(mSharedPreferences.getString("advancedLat", "0"));
            mLongitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", "0"));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            mLocationName = mSharedPreferences.getString("location1", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Long", 0));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            mLocationName = mSharedPreferences.getString("location2", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Long", 0));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            mLocationName = mSharedPreferences.getString("location3", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Long", 0));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            mLocationName = mSharedPreferences.getString("location4", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Long", 0));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            mLocationName = mSharedPreferences.getString("location5", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Long", 0));
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        }
        if ((ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) ||
                mSharedPreferences.getBoolean("useZipcode", false)) {
            mLocationName = mSharedPreferences.getString("oldLocationName", "");
            double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
            double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));

            if (oldLat != 0 && oldLong != 0) {
                mLatitude = oldLat;
                mLongitude = oldLong;
            }
            setElevationFromSP();
            setTimeZoneID();
            return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
        } else {// we are using the devices location
            if (ActivityCompat.checkSelfPermission(mContext, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED || isForWidget) {
                return getLastKnownGeoLocation();
            } else {// this code can only run in a service, broadcast receiver, or some other service that is in the foreground. Widgets can't use this while the app is not in the foreground
                LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && consumer != null) {
                    List<String> providers = locationManager.getAllProviders();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Runnable::run, consumer);
                        }
                        if (providers.contains(LocationManager.GPS_PROVIDER)) {
                            locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Runnable::run, consumer);
                        }
                    } else {
                        if (providers.contains(LocationManager.GPS_PROVIDER)) {
                            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                            if (location == null) {
                                if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                            }
                            if (location != null) {
                                mLatitude = location.getLatitude();
                                mLongitude = location.getLongitude();
                                setElevationFromSP();
                                setTimeZoneID();
                                return new GeoLocation(getLocationAsName(mLatitude, mLongitude), mLatitude, mLongitude, mElevation, mTimeZone);
                            }
                        }
                    }
                }
            }
            return new GeoLocation();// the consumer should update with the actual location
        }
    }

    public @NonNull GeoLocation getLastKnownGeoLocation() {
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            mElevation = 0;
        } else {
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        }
        return new GeoLocation(
                mSharedPreferences.getString("name", ""),
                Double.longBitsToDouble(mSharedPreferences.getLong("Lat", 0)),
                Double.longBitsToDouble(mSharedPreferences.getLong("Long", 0)),
                mElevation,
                TimeZone.getDefault()
        );
    }

    public void resolveElevation(Runnable codeToRunAfter) {
        boolean sUserIsOffline = false;
        if (mLocationName.contains("Lat:") && mLocationName.contains("Long:")
                && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("SetElevationToLastKnownLocation", false)) {//only if the user has enabled the setting to set the elevation to the last known location
            sUserIsOffline = true;
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mSharedPreferences.getString("name", ""), "0"));//lastKnownLocation
        } else {//user is online, get the elevation from the shared preferences for the current location
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationName, "0"));//get the last value of the current location or 0 if it doesn't exist
        }

        if (!sUserIsOffline && mSharedPreferences.getBoolean("useElevation", true)
                && !PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("LuachAmudeiHoraah", false)) {//update if the user is online and the elevation setting is enabled
            if (!mSharedPreferences.contains("elevation" + mLocationName)) {//if the elevation for this location has never been set
                Thread thread = new Thread(() -> getElevationFromWebService(new Handler(Looper.getMainLooper()),
                        () -> mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationName, "0")),
                        codeToRunAfter));
                thread.start();
            } else {// use elevation that was set before
                mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationName, "0"));
                codeToRunAfter.run();
            }
        } else {// user does not want elevation or is in Amudei Horaah mode
            mElevation = 0;
            codeToRunAfter.run();
        }
    }

    public String getLocationAsName(double latitude, double longitude) {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            if (Utils.isLocaleHebrew()) {
                addresses = mGeocoder.getFromLocation(latitude, longitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(latitude, longitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {

            if (Utils.isLocaleHebrew()) {
                Address address = null;
                for (Address add:addresses) {
                    if (add.getLocale().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                        address = add;
                    }
                }
                String city = null;
                if (address != null) {
                    city = address.getLocality();
                }
                if (city != null) {
                    result.append(city).append(", ");
                }

                String state = null;
                if (address != null) {
                    state = address.getAdminArea();
                }
                if (state != null) {
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                if (city == null && state == null) {
                    String country = null;
                    if (address != null) {
                        country = address.getCountryName();
                    }
                    result.append(country);
                }
            } else {
                String city = addresses.get(0).getLocality();
                if (city != null) {
                    result.append(city).append(", ");
                }

                String state = addresses.get(0).getAdminArea();
                if (state != null) {
                    result.append(state);
                }

                if (result.toString().endsWith(", ")) {
                    result.deleteCharAt(result.length() - 2);
                }

                if (city == null && state == null) {
                    String country = addresses.get(0).getCountryName();
                    result.append(country);
                }
            }
        }
        mLocationName = result.toString().trim();
        if (mLocationName.isEmpty()) {
            mLocationName = "Lat: " + String.format(Locale.getDefault(), "%.3f", latitude)
                    + " Long: " + String.format(Locale.getDefault(), "%.3f", longitude);
        }
        mSharedPreferences.edit().putString("name", mLocationName).apply();
        return mLocationName;
    }

    private void setElevationFromSP() {
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            mElevation = 0;
        } else {
            mElevation = Double.parseDouble(mSharedPreferences.getString("elevation" + mLocationName, "0"));//lastKnownLocation
        }
    }

    public double getElevation() {
        return mElevation;
    }

    public TimeZone getTimeZone() {
        setTimeZoneID();
        return mTimeZone;
    }
}
