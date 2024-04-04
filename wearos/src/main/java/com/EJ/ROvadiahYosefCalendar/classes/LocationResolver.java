package com.EJ.ROvadiahYosefCalendar.classes;

import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.SHARED_PREF;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sCurrentLocationName;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sCurrentTimeZoneID;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sLatitude;
import static com.EJ.ROvadiahYosefCalendar.presentation.MainActivity.sLongitude;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;

import com.EJ.ROvadiahYosefCalendar.presentation.MainActivity;
import com.kosherjava.zmanim.util.GeoLocation;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
     * Currently this method will not try to request the user's current location as Google has made it
     * increasingly difficult to request one time location updates on devices with API < 30. If we were
     * to try and pause the app until we get the location like we currently do in the main app, our
     * watch app crashes due to an ANR error with a time limit of 5 seconds (at least that is what
     * seems to be the issue to me). Eventually this method should be changed to actually request the
     * user's location, but I will personally wait until API 30 is the minimum on the play store to
     * adopt that change. For now, we will just receive the current location data from the main app.
     */
    public void acquireLatitudeAndLongitude() {
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
            //TODO when wear os apps have a bare minimum requirement of API 30 and above, look into replacing this with actually getting the watch's location
            sCurrentLocationName = mSharedPreferences.getString("currentLN", "");
            sLatitude = Double.parseDouble(mSharedPreferences.getString("currentLat", "0"));
            sLongitude = Double.parseDouble(mSharedPreferences.getString("currentLong", "0"));
            sCurrentTimeZoneID = mSharedPreferences.getString("currentTimezone", "");
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
                sCurrentTimeZoneID = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(sLatitude, sLongitude)).getZoneId();
            } catch (IllegalArgumentException e) {
                sCurrentTimeZoneID = TimeZone.getDefault().getID();
            }
//        } else {right now the timezone is set by shared preferences, this will need to be reset after API 30
//            MainActivity.sCurrentTimeZoneID = TimeZone.getDefault().getID();
        }
        if (sCurrentTimeZoneID.equals("Asia/Gaza") || sCurrentTimeZoneID.equals("Asia/Hebron")) {
            sCurrentTimeZoneID = "Asia/Jerusalem";
        }
    }

    public static GeoLocation getLastGeoLocation(SharedPreferences mSharedPreferences) {
        String locationName;
        double lat;
        double longitude;
        String timeZone;

        if (mSharedPreferences.getBoolean("useAdvanced", false)) {
            locationName = mSharedPreferences.getString("advancedLN", "");
            lat = Double.parseDouble(mSharedPreferences.getString("advancedLat", ""));
            longitude = Double.parseDouble(mSharedPreferences.getString("advancedLong", ""));
            timeZone = mSharedPreferences.getString("advancedTimezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation1", false)) {
            locationName = mSharedPreferences.getString("location1", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("location1Lat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("location1Long", 0));
            timeZone = mSharedPreferences.getString("location1Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation2", false)) {
            locationName = mSharedPreferences.getString("location2", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("location2Lat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("location2Long", 0));
            timeZone = mSharedPreferences.getString("location2Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation3", false)) {
            locationName = mSharedPreferences.getString("location3", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("location3Lat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("location3Long", 0));
            timeZone = mSharedPreferences.getString("location3Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation4", false)) {
            locationName = mSharedPreferences.getString("location4", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("location4Lat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("location4Long", 0));
            timeZone = mSharedPreferences.getString("location4Timezone", "");
        } else if (mSharedPreferences.getBoolean("useLocation5", false)) {
            locationName = mSharedPreferences.getString("location5", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("location5Lat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("location5Long", 0));
            timeZone = mSharedPreferences.getString("location5Timezone", "");
        } else if (mSharedPreferences.getBoolean("useZipcode", false)) {
            locationName = mSharedPreferences.getString("oldLocationName", "");
            lat = Double.longBitsToDouble(mSharedPreferences.getLong("oldLat", 0));
            longitude = Double.longBitsToDouble(mSharedPreferences.getLong("oldLong", 0));
            try {
                TimeZoneMap timeZoneMap = TimeZoneMap.forRegion(
                        Math.floor(lat), Math.floor(longitude),
                        Math.ceil(lat), Math.ceil(longitude));//trying to avoid using the forEverywhere() method
                timeZone = Objects.requireNonNull(timeZoneMap.getOverlappingTimeZone(lat, longitude)).getZoneId();
            } catch (IllegalArgumentException e) {
                timeZone = TimeZone.getDefault().getID();
            }
        } else {
            //TODO when wear os apps have a bare minimum requirement of API 30 and above, look into replacing this with actually getting the watch's location
            locationName = mSharedPreferences.getString("currentLN", "");
            lat = Double.parseDouble(mSharedPreferences.getString("currentLat", "0"));
            longitude = Double.parseDouble(mSharedPreferences.getString("currentLong", "0"));
            timeZone = mSharedPreferences.getString("currentTimezone", "");
        }
        return new GeoLocation(locationName,lat,longitude, TimeZone.getTimeZone(timeZone
                .replace("Asia/Gaza", "Asia/Jerusalem")
                .replace("Asia/Hebron", "Asia/Jerusalem")
        ));
    }
}
