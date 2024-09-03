package com.ej.rovadiahyosefcalendar.classes;

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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManager;

import org.geonames.WebService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import us.dustinj.timezonemap.TimeZoneMap;

public class LocationResolver extends Thread {

    private final Context mContext;
    private final Activity mActivity;
    private final Geocoder mGeocoder;
    private final SharedPreferences mSharedPreferences;
    private String mLocationName;
    private double mLatitude;
    private double mLongitude;
    private TimeZone mTimeZone;

    public LocationResolver(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
        mGeocoder = new Geocoder(mContext, Locale.getDefault());
        mSharedPreferences = mContext.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
    }

    /**
     * This method gets the devices last known latitude and longitude. It will ask for permission
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
                sLatitude = Double.parseDouble(mSharedPreferences.getString("advancedLat", ""));
                sLongitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", ""));
            } catch (NumberFormatException e) {
                sLatitude = 0.0;
                sLongitude = 0.0;
            }
            sCurrentTimeZoneID = mSharedPreferences.getString("advancedTimezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location1", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Long", 0));
            sCurrentTimeZoneID = mSharedPreferences.getString("location1Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location2", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Long", 0));
            sCurrentTimeZoneID = mSharedPreferences.getString("location2Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location3", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Long", 0));
            sCurrentTimeZoneID = mSharedPreferences.getString("location3Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location4", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Long", 0));
            sCurrentTimeZoneID = mSharedPreferences.getString("location4Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            sCurrentLocationName = mSharedPreferences.getString("location5", "");
            sLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Lat", 0));
            sLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Long", 0));
            sCurrentTimeZoneID = mSharedPreferences.getString("location5Timezone", "");
        } else if (mSharedPreferences.getBoolean("useZipcode", false)) {
            getLatitudeAndLongitudeFromSearchQuery();
        } else {
            if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, 1);
            } else {
                try {
                    LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            Toast.makeText(mContext, mContext.getString(R.string.please_enable_gps), Toast.LENGTH_SHORT).show();
                        }
                        LocationListener locationListener = new LocationListener() {
                            @Override
                            public void onLocationChanged(@NonNull Location location) {}
                            @Override
                            public void onProviderEnabled(@NonNull String provider) {}
                            @Override
                            public void onProviderDisabled(@NonNull String provider) {}
                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {}
                        };
                        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                        }
                        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                            if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                                locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Runnable::run, consumer);
                            }
                            if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Runnable::run, consumer);
                            }
                            if (sLatitude == 0 && sLongitude == 0) {//use the older implementation until the consumer updates the data
                                Location location = null;
                                if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                                }
                                if (location == null && locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                                if (location != null) {
                                    sLatitude = location.getLatitude();
                                    sLongitude = location.getLongitude();
                                }
                            }
                        } else {//older implementation
                            Location location = null;
                            if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                            }
                            if (location == null && locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            }
                            if (location != null) {
                                sLatitude = location.getLatitude();
                                sLongitude = location.getLongitude();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        resolveCurrentLocationName();
    }

    /**
     * Resolves the current location name to be a latitude and longitude if mCurrentLocationName is empty
     * @see MainFragmentManager#sCurrentLocationName
     */
    public void resolveCurrentLocationName() {
        sCurrentLocationName = getLocationAsName();
        if (sCurrentLocationName.isEmpty()) {
            String lat = String.format(Locale.getDefault(), "%.3f", sLatitude);
            String longitude = String.format(Locale.getDefault(), "%.3f", sLongitude);

            sCurrentLocationName = "Lat: " + lat + " Long: " + longitude;
        }
    }

    public String getFullLocationName() {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
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
                    // County equals check to account Rio De Jenairo
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
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {

            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
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
     * specified zip code/area. If it can not find an address it will make a toast saying that an error
     * occurred.
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
            mSharedPreferences.edit().putString("oldZipcode", zipcode).apply();
            mSharedPreferences.edit().putString("oldLocationName", sCurrentLocationName).apply();
            mSharedPreferences.edit().putLong("oldLat", Double.doubleToRawLongBits(sLatitude)).apply();
            mSharedPreferences.edit().putLong("oldLong", Double.doubleToRawLongBits(sLongitude)).apply();
        } else {
            getOldSearchLocation();
        }
    }

    /**
     * This method retrieves the old location data from the devices storage if it has already been
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
     * This method uses the TimeZoneMap to get the current timezone ID based on the latitude and longitude of the device.
     * If the latitude and longitude are not known, it will use the default timezone ID.
     */
    public void setTimeZoneID() {
        if (mSharedPreferences.getBoolean("useAdvanced", false) ||
                mSharedPreferences.getBoolean("useLocation1", false) ||
                mSharedPreferences.getBoolean("useLocation2", false) ||
                mSharedPreferences.getBoolean("useLocation3", false) ||
                mSharedPreferences.getBoolean("useLocation4", false) ||
                mSharedPreferences.getBoolean("useLocation5", false)) {
            return;
        }
        if (mSharedPreferences.getBoolean("useZipcode", false)) {
            try {
                TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                        Math.floor(sLatitude), Math.floor(sLongitude),
                        Math.ceil(sLatitude), Math.ceil(sLongitude));//trying to avoid using the forEverywhere() method
                String zoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(sLatitude, sLongitude)).getZoneId();
                sCurrentTimeZoneID = zoneID;
                mTimeZone = TimeZone.getTimeZone(zoneID);
                saveLocationInformation();
            } catch (IllegalArgumentException e) {
                sCurrentTimeZoneID = TimeZone.getDefault().getID();
                mTimeZone = TimeZone.getDefault();
            }
        } else {
            sCurrentTimeZoneID = TimeZone.getDefault().getID();
            mTimeZone = TimeZone.getDefault();
        }
        if (sCurrentTimeZoneID.equals("Asia/Gaza") || sCurrentTimeZoneID.equals("Asia/Hebron")) {
            sCurrentTimeZoneID = "Asia/Jerusalem";
        }
        if (mTimeZone.getID().equals("Asia/Gaza") || mTimeZone.getID().equals("Asia/Hebron")) {
            mTimeZone = TimeZone.getTimeZone("Asia/Jerusalem");
        }
    }

    public void aquireTimeZoneID() {
        if (!mSharedPreferences.getBoolean("useAdvanced", false)) {
            return;
        }
        try {
            TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                    Math.floor(sLatitude), Math.floor(sLongitude),
                    Math.ceil(sLatitude), Math.ceil(sLongitude));//trying to avoid using the forEverywhere() method
            String zoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(sLatitude, sLongitude)).getZoneId();
            sCurrentTimeZoneID = zoneID;
            mTimeZone = TimeZone.getTimeZone(zoneID);
        } catch (IllegalArgumentException e) {
            sCurrentTimeZoneID = TimeZone.getDefault().getID();
            mTimeZone = TimeZone.getDefault();
        }
        if (sCurrentTimeZoneID.equals("Asia/Gaza") || sCurrentTimeZoneID.equals("Asia/Hebron")) {
            sCurrentTimeZoneID = "Asia/Jerusalem";
        }
        if (mTimeZone.getID().equals("Asia/Gaza") || mTimeZone.getID().equals("Asia/Hebron")) {
            mTimeZone = TimeZone.getTimeZone("Asia/Jerusalem");
        }
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

    public void getElevationFromWebService() throws IOException {
        WebService.setUserName("Elyahu41");
        ArrayList<Integer> elevations = new ArrayList<>();
        int sum = 0;
        int size;
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
            size = elevations.size();
            if (size == 0) {
                size = 1;//edge case if no elevation data is available
            }
        } catch (NumberFormatException ex) {//an error occurred getting the elevation, probably because too many requests were made
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
                size = elevations.size();
                if (size == 0) {
                    size = 1;//edge case if no elevation data is available
                }
            } catch (NumberFormatException ex1) {
                ex.printStackTrace();
                ex1.printStackTrace();
                return;
            }
        }
        mSharedPreferences.edit().putString("elevation" + sCurrentLocationName, String.valueOf(sum / size)).apply();
    }

    @Override
    public void run() {
        try {
            getElevationFromWebService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getRealtimeNotificationData(Consumer<Location> consumer) {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            mLocationName = mSharedPreferences.getString("advancedLN", "");
            mLatitude = Double.parseDouble(mSharedPreferences.getString("advancedLat", ""));
            mLongitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", ""));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("advancedTimezone", ""));
            return;
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            mLocationName = mSharedPreferences.getString("location1", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Long", 0));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location1Timezone", ""));
            return;
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            mLocationName = mSharedPreferences.getString("location2", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Long", 0));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location2Timezone", ""));
            return;
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            mLocationName = mSharedPreferences.getString("location3", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Long", 0));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location3Timezone", ""));
            return;
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            mLocationName = mSharedPreferences.getString("location4", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Long", 0));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location4Timezone", ""));
            return;
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            mLocationName = mSharedPreferences.getString("location5", "");
            mLatitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Lat", 0));
            mLongitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Long", 0));
            mTimeZone = TimeZone.getTimeZone(mSharedPreferences.getString("location5Timezone", ""));
            return;
        }
        if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            mLocationName = mSharedPreferences.getString("oldLocationName", "");
            double oldLat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
            double oldLong = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));

            if (oldLat != 0 && oldLong != 0) {
                mLatitude = oldLat;
                mLongitude = oldLong;
            }
            setTimeZoneID();
        } else {
            try {
                LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {}

                        @Override
                        public void onProviderEnabled(@NonNull String provider) {}

                        @Override
                        public void onProviderDisabled(@NonNull String provider) {}

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}
                    };
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//newer implementation
                        locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, Runnable::run, consumer);
                        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, Runnable::run, consumer);
                    } else if (mLatitude == 0 && mLongitude == 0) {//use the older implementation, until the consumer updates
                        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);//location might be old
                        if (location == null) {
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        if (location != null) {
                            mLatitude = location.getLatitude();
                            mLongitude = location.getLongitude();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getLocationName(double latitude, double longitude) {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            return mLocationName;
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            return mLocationName;
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            return mLocationName;
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            return mLocationName;
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            return mLocationName;
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            return mLocationName;
        } else if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            return mLocationName;
        }

        mLatitude = latitude;
        mLongitude = longitude;

        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
                addresses = mGeocoder.getFromLocation(mLatitude, mLongitude, 5);
            } else {
                addresses = mGeocoder.getFromLocation(mLatitude, mLongitude, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses != null && !addresses.isEmpty()) {

            if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
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
        return result.toString().trim();
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public TimeZone getTimeZone() {
        setTimeZoneID();
        return mTimeZone;
    }
}
