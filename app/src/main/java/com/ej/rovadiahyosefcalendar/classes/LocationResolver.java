package com.ej.rovadiahyosefcalendar.classes;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.db.AppDatabase;
import com.ej.rovadiahyosefcalendar.db.SavedLocation;
import com.ej.rovadiahyosefcalendar.db.SavedLocationDao;
import com.kosherjava.zmanim.util.GeoLocation;

import net.iakovlev.timeshape.TimeZoneEngine;

import org.geonames.GeoNamesException;
import org.geonames.WebService;

import java.io.IOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class LocationResolver {

    // -----------------------------------------------------------------------
    // TimeZoneEngine — shared across all instances
    // -----------------------------------------------------------------------

    public static volatile TimeZoneEngine ENGINE = null;
    private static volatile boolean sTimeZoneEngineHasBeenInitialized = false;

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    private final Context mContext;
    private final Activity mActivity;
    private final Geocoder mGeocoder;
    private final SharedPreferences mSharedPreferences;
    private final SavedLocationDao mLocationDao;

    private String mLocationName;
    private double mLatitude;
    private double mLongitude;
    private double mElevation;
    private TimeZone mTimeZone = TimeZone.getDefault();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public LocationResolver(Context context, Activity activity) {
        mContext         = context;
        mActivity        = activity;
        mGeocoder        = new Geocoder(context, context.getResources()
                                                        .getConfiguration()
                                                        .getLocales()
                                                        .get(0));
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF, Context.MODE_PRIVATE);
        mLocationDao     = AppDatabase.getInstance(context).savedLocationDao();
    }

    // -----------------------------------------------------------------------
    // Public API — location acquisition
    // -----------------------------------------------------------------------

    /**
     * Resolves the active lat/long from:
     * 1. Device GPS (if enabled)
     * 2. Saved location from Room (selectedLocationId)
     * 3. Otherwise fallback to last known GPS
     */
    public void acquireLatitudeAndLongitude(Consumer<Location> consumer) {

        if (mSharedPreferences.getBoolean("useDeviceLocation", false)) {
            acquireDeviceLocation(consumer);
            resolveCurrentLocationName();
            return;
        }

        int selectedLocationId = mSharedPreferences.getInt("selectedLocationId", -1);
        if (selectedLocationId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                SavedLocation loc = mLocationDao.findById(selectedLocationId);
                if (loc != null) {
                    sCurrentLocationName = loc.name;
                    sLatitude            = loc.latitude;
                    sLongitude           = loc.longitude;
                    mLocationDao.updateLastUsed(loc.name, System.currentTimeMillis());
                }
                new Handler(Looper.getMainLooper()).post(this::resolveCurrentLocationName);
            });
            return;
        }

        // Fallback: last known GPS
        resolveCurrentLocationName();
    }

    // -----------------------------------------------------------------------
    // Device location
    // -----------------------------------------------------------------------

    private void acquireDeviceLocation(Consumer<Location> consumer) {
        if (ActivityCompat.checkSelfPermission(mContext, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(mContext, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                    LocationManager.NETWORK_PROVIDER,
                    null,
                    mContext.getMainExecutor(),
                    location -> {
                        if (location != null) {
                            sLatitude  = location.getLatitude();
                            sLongitude = location.getLongitude();
                            consumer.accept(location);
                        }
                    }
            );
        } else {
            Location last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null) {
                sLatitude  = last.getLatitude();
                sLongitude = last.getLongitude();
                consumer.accept(last);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Location name resolution
    // -----------------------------------------------------------------------

    public void resolveCurrentLocationName() {
        getFullLocationName(true, locationName -> {
            if (locationName != null && !locationName.isEmpty()) {
                sCurrentLocationName = locationName;
            } else {
                String lat = String.format(Locale.getDefault(), "%.3f", sLatitude);
                String lon = String.format(Locale.getDefault(), "%.3f", sLongitude);
                sCurrentLocationName = "Lat: " + lat + ", Long: " + lon;
            }
            mSharedPreferences.edit().putString("name", sCurrentLocationName).apply();
        });
    }

    public void getFullLocationName(boolean postalCode, @NonNull LocationNameCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mGeocoder.getFromLocation(sLatitude, sLongitude,
                    1,
                    addresses -> callback.onResult(buildLocationString(addresses, postalCode)));
        } else {
            Executors.newSingleThreadExecutor().execute(() -> {
                String result = null;
                try {
                    List<Address> addresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
                    result = buildLocationString(addresses, postalCode);
                } catch (IOException ignored) {}

                String finalResult = result;
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
            });
        }
    }

	public void getFullLocationName(double latitude, double longitude,
									boolean postalCode, @NonNull LocationNameCallback callback) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			mGeocoder.getFromLocation(latitude, longitude,
				1,
				addresses -> callback.onResult(buildLocationString(addresses, postalCode)));
		} else {
			Executors.newSingleThreadExecutor().execute(() -> {
				String result = null;
				try {
					List<Address> addresses = mGeocoder.getFromLocation(latitude, longitude, 1);
					result = buildLocationString(addresses, postalCode);
				} catch (IOException ignored) {}

				String finalResult = result;
				new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
			});
		}
	}


	@Nullable
    private String buildLocationString(@Nullable List<Address> addresses, boolean postalCode) {
        if (addresses == null || addresses.isEmpty()) return null;

        Address a = addresses.get(0);
        StringBuilder result = new StringBuilder();

        String city = a.getLocality();
        String state = a.getAdminArea();
        String country = a.getCountryName();

        if (city != null) result.append(city);
        if (state != null) {
            if (result.length() > 0) result.append(", ");
            result.append(state);
        }
        if (result.length() == 0 && country != null) result.append(country);

        if (postalCode && a.getPostalCode() != null) {
            result.append(" (").append(a.getPostalCode()).append(")");
        }

        return result.toString().trim();
    }

    // -----------------------------------------------------------------------
    // Search query → lat/long
    // -----------------------------------------------------------------------

    public boolean getLatitudeAndLongitudeFromSearchQuery(String query) {
        List<Address> addresses = null;
        try {
            addresses = mGeocoder.getFromLocationName(query, 1);
        } catch (IOException ignored) {}

        if (addresses != null && !addresses.isEmpty()) {
            Address first = addresses.get(0);
            sLatitude  = first.getLatitude();
            sLongitude = first.getLongitude();

            try {
                List<Address> nameAddresses = mGeocoder.getFromLocation(sLatitude, sLongitude, 1);
                String resolved = buildLocationString(nameAddresses, true);
                if (resolved != null && !resolved.isEmpty()) {
                    sCurrentLocationName = resolved;
                }
            } catch (IOException ignored) {}

            return true;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // TimeZoneEngine
    // -----------------------------------------------------------------------

    public static void initializeTimeshapeEngine() {
        if (!sTimeZoneEngineHasBeenInitialized) {
            sTimeZoneEngineHasBeenInitialized = true;
            new Thread(() -> ENGINE = TimeZoneEngine.initialize()).start();
        }
    }

    private static TimeZoneEngine awaitTimeshapeEngine() {
        initializeTimeshapeEngine();
        while (ENGINE == null) {
            Thread.yield();
        }
        return ENGINE;
    }

    /**
     * Synchronous version — call ONLY from a background thread.
     * Used by notification services where the result is needed immediately.
     */
    public void setTimeZoneID() {
        int selectedId = mSharedPreferences.getInt("selectedLocationId", -1);
        if (selectedId != -1) {
            SavedLocation loc = mLocationDao.findById(selectedId); // must be on BG thread
            if (loc != null && loc.timezoneId != null && !loc.timezoneId.equals("GMT")) {
                mTimeZone = TimeZone.getTimeZone(loc.timezoneId);
                normalizeGazaTimezone();
                sCurrentTimeZoneID = mTimeZone.getID();
                return;
            }
        }
        resolveTimezoneFromCoordinates();
    }

    /**
     * Async version — safe to call from the main thread.
     * Used by ZmanimFragment and any UI code that can't block.
     */
    public void setTimeZoneIDAsync(Runnable onComplete) {
        int selectedId = mSharedPreferences.getInt("selectedLocationId", -1);
        if (selectedId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                SavedLocation loc = mLocationDao.findById(selectedId);
                if (loc != null && loc.timezoneId != null && !loc.timezoneId.equals("GMT")) {
                    mTimeZone = TimeZone.getTimeZone(loc.timezoneId);
                    normalizeGazaTimezone();
                    sCurrentTimeZoneID = mTimeZone.getID();
                } else {
                    resolveTimezoneFromCoordinates();
                }
                if (onComplete != null) {
                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            });
            return;
        }
        // No selectedId — coordinate resolution doesn't touch the DB, safe to run inline
        Executors.newSingleThreadExecutor().execute(() -> {
            resolveTimezoneFromCoordinates();
            if (onComplete != null) {
                new Handler(Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    private void resolveTimezoneFromCoordinates() {
        if (sLatitude != 0.0 && sLongitude != 0.0) {
            try {
                mTimeZone = TimeZone.getTimeZone(resolveZoneId(sLatitude, sLongitude));
            } catch (IllegalArgumentException e) {
                mTimeZone = TimeZone.getDefault();
            }
        } else {
            mTimeZone = TimeZone.getDefault();
        }
        normalizeGazaTimezone();
        sCurrentTimeZoneID = mTimeZone.getID();
    }

    private String resolveZoneId(double latitude, double longitude) {
        List<ZoneId> allZones = awaitTimeshapeEngine().queryAll(latitude, longitude);
        if (allZones.isEmpty()) return TimeZone.getDefault().getID();

        String deviceZone = TimeZone.getDefault().getID();
        for (ZoneId zone : allZones) {
            if (zone.toString().equals(deviceZone)) return deviceZone;
        }
        return allZones.get(0).toString();
    }

    private void normalizeGazaTimezone() {
        if ("Asia/Gaza".equals(mTimeZone.getID()) || "Asia/Hebron".equals(mTimeZone.getID())) {
            mTimeZone = TimeZone.getTimeZone("Asia/Jerusalem");
        }
    }

	public double getElevation() {
		return mElevation;
	}

	public TimeZone getResolvedTimeZone() {
		return mTimeZone;
	}


	// -----------------------------------------------------------------------
    // Saved-location persistence (Room)
    // -----------------------------------------------------------------------

    private void saveLocationToDatabase() {
        if (sCurrentLocationName == null || sCurrentLocationName.isEmpty()) return;

        SavedLocation existing = mLocationDao.findByName(sCurrentLocationName);
        if (existing != null) {
            mLocationDao.updateLastUsed(sCurrentLocationName, System.currentTimeMillis());
            return;
        }

        if (mLocationDao.count() >= AppDatabase.MAX_SAVED_LOCATIONS) {
            mLocationDao.deleteOldest();
        }

        SavedLocation newLocation = new SavedLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                sCurrentTimeZoneID,
                System.currentTimeMillis()
        );
        mLocationDao.insert(newLocation);
    }

    // -----------------------------------------------------------------------
    // Elevation
    // -----------------------------------------------------------------------

    public void resolveElevation(Runnable codeToRunAfter) {
        mElevation = Double.parseDouble(
                mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));

        boolean elevationEnabled = mSharedPreferences.getBoolean("useElevation", true);
        boolean amudeiHoraah     = PreferenceManager.getDefaultSharedPreferences(mContext)
                                                    .getBoolean("LuachAmudeiHoraah", false);

        if (elevationEnabled && !amudeiHoraah &&
            !mSharedPreferences.contains("elevation" + sCurrentLocationName)) {

            new Thread(() -> getElevationFromWebService(
                    new Handler(Looper.getMainLooper()),
                    () -> mElevation = Double.parseDouble(
                            mSharedPreferences.getString("elevation" + sCurrentLocationName, "0")),
                    codeToRunAfter)
            ).start();

        } else {
            codeToRunAfter.run();
        }
    }

    public void getElevationFromWebService(Handler handler,
                                           Runnable codeToRunInBackground,
                                           Runnable codeToRunOnMainThread) {
        double elevation = fetchAverageElevation();
        mSharedPreferences.edit()
                .putString("elevation" + sCurrentLocationName, String.valueOf(elevation))
                .apply();

        if (codeToRunInBackground != null) {
            try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
                Future<?> future = executor.submit(codeToRunInBackground);
                executor.execute(() -> {
                    try {
                        future.get();
                        handler.post(codeToRunOnMainThread);
                    } catch (Exception ignored) {}
                });
            }
        } else {
            handler.post(codeToRunOnMainThread);
        }
    }

    private double fetchAverageElevation() {
        ArrayList<Integer> elevations = new ArrayList<>();
        try {
            elevations = queryGeoNames("Elyahu41");
        } catch (Exception ignored) {
            try {
                elevations = queryGeoNames("graviton57");
            } catch (Exception ignored2) {}
        }
        if (elevations.isEmpty()) return 0;
        int sum = 0;
        for (int e : elevations) sum += e;
        return (double) sum / elevations.size();
    }

    private ArrayList<Integer> queryGeoNames(String userName)
            throws GeoNamesException, NumberFormatException, IOException {
        WebService.setUserName(userName);
        ArrayList<Integer> result = new ArrayList<>();
        int e1 = WebService.srtm3(sLatitude, sLongitude);
        if (e1 > 0) result.add(e1);
        int e2 = WebService.astergdem(sLatitude, sLongitude);
        if (e2 > 0) result.add(e2);
        int e3 = WebService.gtopo30(sLatitude, sLongitude);
        if (e3 > 0) result.add(e3);
        return result;
    }

    // -----------------------------------------------------------------------
    // Real-time / notification GeoLocation
    // -----------------------------------------------------------------------

    public GeoLocation getRealtimeNotificationData(Consumer<Location> consumer, boolean isForWidget) {

        if (mSharedPreferences.getBoolean("useDeviceLocation", false)) {
            return getLastKnownGeoLocation();
        }

        int selectedId = mSharedPreferences.getInt("selectedLocationId", -1);
        if (selectedId != -1) {
            SavedLocation loc = null;
            try {
                loc = Executors.newSingleThreadExecutor().submit(() ->
                    mLocationDao.findById(selectedId)
                ).get();
            } catch (Exception ignored) {}

            if (loc != null) {
                mLocationName = loc.name;
                mLatitude     = loc.latitude;
                mLongitude    = loc.longitude;
                mTimeZone     = TimeZone.getTimeZone(loc.timezoneId);
                normalizeGazaTimezone();
                sCurrentTimeZoneID = mTimeZone.getID();
                setElevationFromSP();
                return new GeoLocation(mLocationName, mLatitude, mLongitude, mElevation, mTimeZone);
            }
        }

        return getLastKnownGeoLocation();
    }

    public String getLocationNameSync(double latitude, double longitude) {
        try {
            List<Address> addresses = mGeocoder.getFromLocation(latitude, longitude, 1);
            return buildLocationString(addresses, true);
        } catch (Exception e) {
            return null;
        }
    }

    public GeoLocation getLastKnownGeoLocation() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(mContext, ACCESS_BACKGROUND_LOCATION) != PERMISSION_GRANTED) {
            return new GeoLocation(sCurrentLocationName, sLatitude, sLongitude, mElevation, mTimeZone);
        }

        if (locationManager != null) {
            Location last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null) {
                return new GeoLocation(
                        sCurrentLocationName,
                        last.getLatitude(),
                        last.getLongitude(),
                        mElevation,
                        mTimeZone
                );
            }
        }

        return new GeoLocation(sCurrentLocationName, sLatitude, sLongitude, mElevation, mTimeZone);
    }

    private void setElevationFromSP() {
        mElevation = Double.parseDouble(
                mSharedPreferences.getString("elevation" + sCurrentLocationName, "0"));
    }

    // -----------------------------------------------------------------------
    // Callback interface
    // -----------------------------------------------------------------------

    public interface LocationNameCallback {
        void onResult(String name);
    }
}