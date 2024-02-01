package com.EJ.ROvadiahYosefCalendar.classes;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sCurrentLocationName;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sLatitude;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sLongitude;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sElevation;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sCurrentTimeZoneID;

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

import com.EJ.ROvadiahYosefCalendar.R;
import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import us.dustinj.timezonemap.TimeZoneMap;

public class LocationResolver {

    private final Context mContext;
    private final Activity mActivity;
    private final Geocoder mGeocoder;
    private final SharedPreferences mSharedPreferences;

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
    @SuppressWarnings("BusyWait")
    public void acquireLatitudeAndLongitude() {
        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            sCurrentLocationName = mSharedPreferences.getString("advancedLN", "");
            sLatitude = Double.parseDouble(mSharedPreferences.getString("advancedLat", ""));
            sLongitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", ""));
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
            if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{ACCESS_FINE_LOCATION}, 1);
            } else {
                try {
                    LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager != null) {
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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
                                locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                sLatitude = location.getLatitude();
                                                sLongitude = location.getLongitude();
                                            }
                                        });
                            }
                            if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER,
                                        null, Runnable::run,
                                        location -> {
                                            if (location != null) {
                                                sLatitude = location.getLatitude();
                                                sLongitude = location.getLongitude();
                                            }
                                        });
                            }
                            long tenSeconds = System.currentTimeMillis() + 10000;
                            while ((sLatitude == 0.0 && sLongitude == 0.0) && System.currentTimeMillis() < tenSeconds) {
                                Thread.sleep(0);//we MUST wait for the location data to be set or else the app will crash
                            }
                            if (sLatitude == 0.0 && sLongitude == 0.0) {//if 10 seconds passed and we still don't have the location, use the older implementation
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
        setTimeZoneID();
        resolveCurrentLocationName();
    }

    /**
     * Resolves the current location name to be a latitude and longitude if mCurrentLocationName is empty
     * @see MainActivity#sCurrentLocationName
     */
    public void resolveCurrentLocationName() {
        sCurrentLocationName = getLocationAsName();
        if (sCurrentLocationName.isEmpty()) {
            String lat = String.format(Locale.getDefault(), "%.3f", sLatitude);
            String longitude = String.format(Locale.getDefault(), "%.3f", sLongitude);

            sCurrentLocationName = "Lat: " + lat + " Long: " + longitude;
        }
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
        if (addresses != null && addresses.size() > 0) {

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
                MainActivity.sCurrentTimeZoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(sLatitude, sLongitude)).getZoneId();
            } catch (IllegalArgumentException e) {
                MainActivity.sCurrentTimeZoneID = TimeZone.getDefault().getID();
            }
        } else {
            MainActivity.sCurrentTimeZoneID = TimeZone.getDefault().getID();
        }
    }
}
