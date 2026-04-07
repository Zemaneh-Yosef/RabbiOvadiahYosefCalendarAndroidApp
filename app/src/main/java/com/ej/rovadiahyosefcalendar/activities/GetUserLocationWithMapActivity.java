package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.classes.Utils;
import com.ej.rovadiahyosefcalendar.databinding.ActivityGetUserLocationWithMapBinding;
import com.ej.rovadiahyosefcalendar.db.AppDatabase;
import com.ej.rovadiahyosefcalendar.db.SavedLocation;
import com.ej.rovadiahyosefcalendar.db.SavedLocationDao;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.SphericalUtil;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class GetUserLocationWithMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityGetUserLocationWithMapBinding binding;
    private SharedPreferences mSharedPreferences;
    private SavedLocationDao mLocationDao;
    private LatLng mChosenLatLng;
    private ItemAdapter mItemAdapter;
    private Marker mCurrentMarker;
    private LocationResolver mLocationResolver;

    // -----------------------------------------------------------------------
    // Back-navigation backup — only two fields now instead of eight
    // -----------------------------------------------------------------------
    private String  bLocationName;
    private double  bLat;
    private double  bLong;
    private String  bTimezoneID;
    private boolean bUseDeviceLocation; // true  → follow GPS
    private int     bSelectedLocationId; // Room PK, -1 → device location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationResolver.initializeTimeshapeEngine();

        binding = ActivityGetUserLocationWithMapBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        if (Utils.isLocaleHebrew(this)) {
            binding.topAppBar.setSubtitle("");
        }

        binding.topAppBar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        binding.topAppBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.search_for_a_place_legacy) {
                showSearchDialog();
                return true;
            } else if (id == R.id.advanced) {
                showAdvancedDialog();
                return true;
            } else if (id == R.id.skip) {
                finish();
                return true;
            }
            return false;
        });

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        mLocationDao = AppDatabase.getInstance(this).savedLocationDao();

        // Snapshot current state for back-navigation restore
        bLocationName      = sCurrentLocationName;
        bLat               = sLatitude;
        bLong              = sLongitude;
        bTimezoneID        = sCurrentTimeZoneID;
        bUseDeviceLocation = mSharedPreferences.getBoolean("useDeviceLocation", false);
        bSelectedLocationId = mSharedPreferences.getInt("selectedLocationId", -1);

        initMap();
        initDeviceLocationButton();
        initSearchView();
        initConfirmButton();
        initWindowInsets();
        initBackHandler();
        loadSavedLocationsIntoList();
    }

    // -----------------------------------------------------------------------
    // Initialisation helpers
    // -----------------------------------------------------------------------

    private void initMap() {
        try {
            SupportMapFragment mapFragment = (SupportMapFragment)
                    getSupportFragmentManager().findFragmentById(R.id.map);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            Toast.makeText(this, t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initDeviceLocationButton() {
        binding.deviceLocation.setOnClickListener(v -> {
            if (mMap == null) return;
            clearMarker();
            selectDeviceLocation();

            mLocationResolver = new LocationResolver(this, this);
            mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());

            // Wait off the main thread until GPS provides coordinates
            Executors.newSingleThreadExecutor().execute(() -> {
                while (sLatitude == 0 && sLongitude == 0) Thread.yield();
                runOnUiThread(() -> {
                    mChosenLatLng = new LatLng(sLatitude, sLongitude);
                    mLocationResolver.getFullLocationName(true, name -> {
                        if (name != null) {
                            runOnUiThread(() -> mCurrentMarker = mMap.addMarker(
                                    new MarkerOptions().position(mChosenLatLng).draggable(true).title(name)));
                        }
                    });
                    animateMapTo(mChosenLatLng);
                    showSnackbar(getString(R.string.the_application_will_keep_requesting_your_location),
                            getColor(R.color.green), getColor(R.color.black));
                });
            });
        });
    }

    private void initSearchView() {
        binding.searchRV.setLayoutManager(new LinearLayoutManager(this));
        binding.searchRV.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                clearMarker();
                binding.searchView.clearFocus();

                // Resolve query → SavedLocation on a background thread
                Executors.newSingleThreadExecutor().execute(() -> {
                    mLocationResolver = new LocationResolver(
                            GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                    boolean success = mLocationResolver.getLatitudeAndLongitudeFromSearchQuery(query);

                    if (success) {
                        // 1. Resolve timezone from coordinates BEFORE saving or selecting
                        //    Temporarily clear selectedLocationId so setTimeZoneID() doesn't
                        //    short-circuit on the stale DB value
                        mSharedPreferences.edit().remove("selectedLocationId").commit(); // commit(), not apply()
                        mLocationResolver.setTimeZoneID();

                        // 2. Now save with the correct timezone already in sCurrentTimeZoneID
                        SavedLocation saved = saveCurrentStateAsLocation();

                        // 3. Mark as selected (writes the correct ID back)
                        selectSavedLocation(saved);

                        runOnUiThread(() -> {
                            mChosenLatLng = new LatLng(sLatitude, sLongitude);
                            if (mMap != null) {
                                mLocationResolver.getFullLocationName(true, name -> {
                                    if (name != null) {
                                        runOnUiThread(() -> mCurrentMarker = mMap.addMarker(
                                            new MarkerOptions().position(mChosenLatLng).draggable(true).title(name)));
                                    }
                                });
                                animateMapTo(mChosenLatLng);
                            }
                            showSnackbar(getString(R.string.the_application_will_not_track_your_location),
                                Color.RED, Color.WHITE);
                            refreshRecyclerView();
                        });
                    }
                });
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                refreshRecyclerView(newText);
                return false;
            }
        });
    }

    private void initConfirmButton() {
        binding.confirmLocation.setOnClickListener(v -> {
            if (mChosenLatLng == null) {
                showSnackbar(getString(R.string.no_location_has_been_set), Color.RED, Color.WHITE);
                return;
            }
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.confirmLocation.setEnabled(false);

            Runnable finish = () -> {
                // setTimeZoneID / acquireTimeZoneID are blocking — run on BG thread
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (!mSharedPreferences.getBoolean("useDeviceLocation", false)) {
                        mLocationResolver.setTimeZoneID();
                    }
                    // If this was a map-tap (advanced), acquire timezone from coordinates
                    // acquireTimeZoneID is idempotent; setTimeZoneID already handled others
                    runOnUiThread(() -> {
                        configureSettingsBasedOnLocation();
                        finish();
                    });
                });
            };

            if (mSharedPreferences.getBoolean("useElevation", true)
                    && !mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
                new Thread(() -> mLocationResolver.getElevationFromWebService(
                        new Handler(getMainLooper()), null, finish)).start();
            } else {
                finish.run();
            }
        });
    }

    private void initWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.confirmLocation, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin   = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin  = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Restore everything that was snapshotted in onCreate
                sCurrentLocationName = bLocationName;
                sLatitude            = bLat;
                sLongitude           = bLong;
                sCurrentTimeZoneID   = bTimezoneID;
                mSharedPreferences.edit()
                        .putBoolean("useDeviceLocation",  bUseDeviceLocation)
                        .putInt("selectedLocationId",     bSelectedLocationId)
                        .apply();
                finish();
                if (!getIntent().getBooleanExtra("loneActivity", false)) {
                    startActivity(new Intent(getApplicationContext(), WelcomeScreenActivity.class));
                }
            }
        });
    }

    /**
     * Loads the saved-location list from Room on a background thread,
     * then hands the result to the RecyclerView adapter on the main thread.
     */
    private void loadSavedLocationsIntoList() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SavedLocation> locations = mLocationDao.getAllOrderedByRecent();
            runOnUiThread(() -> {
                mItemAdapter = new ItemAdapter(locations);
                binding.searchRV.setAdapter(mItemAdapter);
            });
        });
    }

    // -----------------------------------------------------------------------
    // Map callbacks
    // -----------------------------------------------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        DisplayMetrics dm = new DisplayMetrics();
        if (getWindowManager() != null) {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            mMap.setPadding(dm.widthPixels / 10, dm.heightPixels / 10,
                            dm.widthPixels / 10, dm.heightPixels / 10);
        }

        if (sLatitude != 0 && sLongitude != 0) {
            LatLng current = new LatLng(sLatitude, sLongitude);
            mCurrentMarker = mMap.addMarker(
                    new MarkerOptions().position(current).draggable(true).title(sCurrentLocationName));
            animateMapTo(current);
        }

        mMap.setOnMapClickListener(latLng -> {
            clearMarker();
            mChosenLatLng = latLng;
            sLatitude  = latLng.latitude;
            sLongitude = latLng.longitude;

            mLocationResolver = new LocationResolver(this, this);

            Executors.newSingleThreadExecutor().execute(() -> {
                mLocationResolver.setTimeZoneID();

                // now resolve the name
                mLocationResolver.getFullLocationName(true, name -> {
                    if (name != null) {
                        sCurrentLocationName = name;

                        runOnUiThread(() -> mCurrentMarker = mMap.addMarker(
                            new MarkerOptions().position(latLng).draggable(true).title(name)));

                        Executors.newSingleThreadExecutor().execute(() -> {
                            SavedLocation saved = saveCurrentStateAsLocation();
                            selectSavedLocation(saved);
                        });
                    }
                });
            });

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            showSnackbar(getString(R.string.the_application_will_not_track_your_location),
                    Color.RED, Color.WHITE);
        });
    }

    // -----------------------------------------------------------------------
    // Dialogs
    // -----------------------------------------------------------------------

    /**
     * The old "zipcode" dialog. Now shows whatever is in the Room DB dynamically
     * instead of five hardcoded buttons.
     */
    private void showSearchDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint(R.string.enter_zipcode_or_address);
        input.setSingleLine();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // Saved locations are loaded from Room and shown as a RecyclerView inside the dialog
        RecyclerView dialogRV = new RecyclerView(this);
        dialogRV.setLayoutManager(new LinearLayoutManager(this));
        dialogRV.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        layout.addView(dialogRV);
        layout.addView(input);

        AlertDialog ad = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.search_for_a_place)
                .setMessage(R.string.warning_zmanim_will_be_based_on_your_approximate_area)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (query.isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        showSearchDialog();
                    } else {
                        resolveSearchQueryAndFinish(query);
                    }
                })
                .setNegativeButton(R.string.advanced, (dialog, which) -> showAdvancedDialog())
                .setNeutralButton(R.string.use_location, (dialog, which) -> resolveDeviceLocationAndFinish())
                .create();

        // Populate the dialog's RecyclerView from Room
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SavedLocation> locations = mLocationDao.getAllOrderedByRecent();
            runOnUiThread(() -> {
                ItemAdapter dialogAdapter = new ItemAdapter(locations) {
                    @Override
                    void onLocationSelected(SavedLocation loc) {
                        ad.dismiss();
                        selectSavedLocation(loc);
                        applySelectedLocationAndFinish();
                    }
                };
                dialogRV.setAdapter(dialogAdapter);
            });
        });

        ad.show();

        input.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                ad.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
    }

    private void showAdvancedDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView tvName = new TextView(this);
        tvName.setText(R.string.enter_location_name);
        tvName.setGravity(Gravity.CENTER);
        EditText etName = new EditText(this);
        etName.setText(sCurrentLocationName);
        etName.setHint(R.string.location_hint);
        etName.setGravity(Gravity.CENTER);

        TextView tvLat = new TextView(this);
        tvLat.setText(R.string.enter_latitude);
        tvLat.setGravity(Gravity.CENTER);
        EditText etLat = new EditText(this);
        etLat.setText(String.valueOf(sLatitude));
        etLat.setHint("ex: 73.09876543");
        etLat.setGravity(Gravity.CENTER);

        TextView tvLong = new TextView(this);
        tvLong.setText(R.string.enter_longitude);
        tvLong.setGravity(Gravity.CENTER);
        EditText etLong = new EditText(this);
        etLong.setText(String.valueOf(sLongitude));
        etLong.setHint("ex: -103.098765");
        etLong.setGravity(Gravity.CENTER);

        TextView tvElev = new TextView(this);
        tvElev.setText(R.string.enter_elevation_in_meters);
        tvElev.setGravity(Gravity.CENTER);
        EditText etElev = new EditText(this);
        etElev.setText(String.valueOf(sElevation));
        etElev.setHint("ex: 805");
        etElev.setGravity(Gravity.CENTER);

        TextView tvTz = new TextView(this);
        tvTz.setText(R.string.choose_timezone);
        tvTz.setGravity(Gravity.CENTER);
        Spinner spinnerTz = new Spinner(this);
        String[] tzIds = TimeZone.getAvailableIDs();
        spinnerTz.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tzIds));
        final String[] chosenTzId = {TimeZone.getDefault().getID()};
        spinnerTz.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                chosenTzId[0] = tzIds[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        layout.addView(tvName);   layout.addView(etName);
        layout.addView(tvLat);    layout.addView(etLat);
        layout.addView(tvLong);   layout.addView(etLong);
        layout.addView(tvElev);   layout.addView(etElev);
        layout.addView(tvTz);     layout.addView(spinnerTz);
        scrollView.addView(layout);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.advanced)
                .setView(scrollView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        showSearchDialog();
                        return;
                    }
                    try {
                        double lat  = Double.parseDouble(etLat.getText().toString().trim());
                        double lng  = Double.parseDouble(etLong.getText().toString().trim());
                        double elev = Double.parseDouble(etElev.getText().toString().trim());

                        sCurrentLocationName = name;
                        sLatitude  = lat;
                        sLongitude = lng;
                        sCurrentTimeZoneID = chosenTzId[0];

                        // Store the manually entered elevation under the chosen name
                        mSharedPreferences.edit()
                                .putString("elevation" + name, String.valueOf(elev))
                                .apply();

                        // Persist as a SavedLocation and select it
                        Executors.newSingleThreadExecutor().execute(() -> {
                            SavedLocation saved = new SavedLocation(
                                    name, lat, lng, chosenTzId[0], System.currentTimeMillis());
                            long rowId = mLocationDao.insert(saved);
                            if (rowId == -1) { // already exists — just refresh timestamp
                                mLocationDao.updateLastUsed(name, System.currentTimeMillis());
                                saved = mLocationDao.findByName(name);
                            }
                            selectSavedLocation(saved);
                            runOnUiThread(() -> {
                                mLocationResolver = new LocationResolver(this, this);
                                mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
                                configureSettingsBasedOnLocation();
                                finish();
                            });
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // -----------------------------------------------------------------------
    // Location selection helpers
    // -----------------------------------------------------------------------

    /**
     * Marks the given SavedLocation as the active one in SharedPreferences.
     * Two fields replace the old 7-boolean mess.
     */
    private void selectSavedLocation(@NonNull SavedLocation loc) {
        mSharedPreferences.edit()
                .putBoolean("useDeviceLocation", false)
                .putInt("selectedLocationId", loc.id)
                .apply();
        sCurrentLocationName = loc.name;
        sLatitude            = loc.latitude;
        sLongitude           = loc.longitude;
        sCurrentTimeZoneID   = loc.timezoneId;
        mLocationDao.updateLastUsed(loc.name, System.currentTimeMillis());
    }

    /** Switches to live device GPS. Clears any saved-location selection. */
    private void selectDeviceLocation() {
        mSharedPreferences.edit()
                .putBoolean("useDeviceLocation", true)
                .putInt("selectedLocationId", -1)
                .apply();
    }

    /**
     * Builds a {@link SavedLocation} from the current global state
     * (sLatitude / sLongitude / sCurrentLocationName / sCurrentTimeZoneID),
     * inserts or updates it in Room, and returns it.
     *
     * Must be called from a background thread.
     */
    @NonNull
    private SavedLocation saveCurrentStateAsLocation() {

        // Evict if over cap
        if (mLocationDao.count() >= AppDatabase.MAX_SAVED_LOCATIONS) {
            mLocationDao.deleteOldest();
        }

        SavedLocation existing = mLocationDao.findByName(sCurrentLocationName);

        long ts = System.currentTimeMillis();

        if (existing != null) {
            // Update all fields, not just lastUsedAt
            mLocationDao.updateLocation(
                sCurrentLocationName,
                sLatitude,
                sLongitude,
                sCurrentTimeZoneID,
                ts
            );
            return mLocationDao.findByName(sCurrentLocationName);
        }

        // Insert new row
        SavedLocation loc = new SavedLocation(
            sCurrentLocationName,
            sLatitude,
            sLongitude,
            sCurrentTimeZoneID,
            ts
        );

        long rowId = mLocationDao.insert(loc);
        loc.id = (int) rowId;
        return loc;
    }


    // -----------------------------------------------------------------------
    // Finish flows
    // -----------------------------------------------------------------------

    private void resolveSearchQueryAndFinish(String query) {
        Executors.newSingleThreadExecutor().execute(() -> {
            mLocationResolver = new LocationResolver(this, this);
            mLocationResolver.getLatitudeAndLongitudeFromSearchQuery(query);
            mLocationResolver.setTimeZoneID();
            SavedLocation saved = saveCurrentStateAsLocation();
            selectSavedLocation(saved);
            runOnUiThread(() -> applySelectedLocationAndFinish());
        });
    }

    private void resolveDeviceLocationAndFinish() {
        selectDeviceLocation();
        mLocationResolver = new LocationResolver(this, this);
        mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
        mLocationResolver.setTimeZoneID();
        applySelectedLocationAndFinish();
    }

    private void applySelectedLocationAndFinish() {
        Runnable finish = () -> {
            configureSettingsBasedOnLocation();
            finish();
        };
        if (mSharedPreferences.getBoolean("useElevation", true)
                && !mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
            new Thread(() -> mLocationResolver.getElevationFromWebService(
                    new Handler(getMainLooper()), null, finish)).start();
        } else {
            finish.run();
        }
    }

    // -----------------------------------------------------------------------
    // RecyclerView refresh
    // -----------------------------------------------------------------------

    private void refreshRecyclerView() {
        refreshRecyclerView(null);
    }

    /**
     * Reloads the saved-location list from Room. When {@code filter} is null or
     * empty the list is hidden; otherwise it is shown (matching the old behaviour
     * of only showing suggestions while the user is typing).
     */
    private void refreshRecyclerView(String filter) {
        if (filter != null && filter.isEmpty()) {
            binding.searchRV.setVisibility(View.GONE);
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            List<SavedLocation> locations = mLocationDao.getAllOrderedByRecent();
            runOnUiThread(() -> {
                if (locations.isEmpty()) {
                    binding.searchRV.setVisibility(View.GONE);
                    return;
                }
                mItemAdapter = new ItemAdapter(locations);
                // Attach swipe-to-delete
                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                        0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override public boolean onMove(@NonNull RecyclerView rv,
                            @NonNull RecyclerView.ViewHolder vh,
                            @NonNull RecyclerView.ViewHolder t) { return false; }

                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                        int pos = vh.getAbsoluteAdapterPosition();
                        SavedLocation loc = mItemAdapter.getItem(pos);
                        Executors.newSingleThreadExecutor().execute(() ->
                                mLocationDao.deleteById(loc.id));
                        mItemAdapter.removeItem(pos);
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.location_deleted), Snackbar.LENGTH_SHORT).show();
                    }
                }).attachToRecyclerView(binding.searchRV);

                binding.searchRV.setAdapter(mItemAdapter);
                binding.searchRV.setVisibility(View.VISIBLE);
            });
        });
    }

    // -----------------------------------------------------------------------
    // Map / UI helpers
    // -----------------------------------------------------------------------

    private void clearMarker() {
        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
            mCurrentMarker = null;
        }
    }

    private void animateMapTo(LatLng center) {
        LatLng ne = SphericalUtil.computeOffset(center, 9500, 45.0);
        LatLng sw = SphericalUtil.computeOffset(center, 9500, 225.0);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(sw, ne), 0));
    }

    private void showSnackbar(String message, int bgColor, int textColor) {
        Snackbar sb = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT);
        sb.setBackgroundTint(bgColor);
        if (textColor != 0) sb.setTextColor(textColor);
        sb.show();
    }

    private void configureSettingsBasedOnLocation() {
        if (sLatitude < 29.4 || sLatitude > 33.3 || sLongitude < 34.0 || sLongitude > 38.5) {
            mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
        }
        if (Utils.isInOrNearIsrael(sLatitude, sLongitude)) {
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                startActivity(new Intent(this, InIsraelActivity.class));
            }
        } else if (!Utils.isLocaleHebrew(this)) {
            mSharedPreferences.edit()
                    .putBoolean("LuachAmudeiHoraah", true)
                    .putBoolean("useElevation", false).apply();
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                startActivity(new Intent(this, ZmanimLanguageActivity.class));
            }
        } else {
            mSharedPreferences.edit().putBoolean("useElevation", false).apply();
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
            }
            mSharedPreferences.edit()
                    .putBoolean("LuachAmudeiHoraah", true)
                    .putBoolean("isZmanimInHebrew", true)
                    .putBoolean("isZmanimEnglishTranslated", false)
                    .putBoolean("isSetup", true).apply();
            if (mSharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
                startActivity(new Intent(getBaseContext(), TipScreenActivity.class));
                mSharedPreferences.edit().putBoolean("hasNotShownTipScreen", false).apply();
            }
        }
    }

    @Override
    protected void onStart() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
        super.onStart();
    }

    // -----------------------------------------------------------------------
    // Permission result
    // -----------------------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            clearMarker();
            LocationResolver locationResolver = new LocationResolver(this, this);
            locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());

            Executors.newSingleThreadExecutor().execute(() -> {
                while (sLatitude == 0 && sLongitude == 0) Thread.yield();
                runOnUiThread(() -> {
                    mChosenLatLng = new LatLng(sLatitude, sLongitude);
                    locationResolver.getFullLocationName(true, name -> {
                        if (name != null) {
                            runOnUiThread(() -> mCurrentMarker = mMap.addMarker(
                                    new MarkerOptions().position(mChosenLatLng).draggable(true).title(name)));
                        }
                    });
                    animateMapTo(mChosenLatLng);
                    showSnackbar(getString(R.string.the_application_will_keep_requesting_your_location),
                            getColor(R.color.green), getColor(R.color.black));
                });
            });
        }
    }

    // -----------------------------------------------------------------------
    // RecyclerView adapter
    // -----------------------------------------------------------------------

    /**
     * Adapter backed by a list of {@link SavedLocation} objects from Room.
     * Replaces the old String-list adapter that had to reverse-engineer which
     * SharedPreferences slot a name belonged to.
     *
     * The {@link #onLocationSelected} method is overridable so the dialog
     * version of this adapter can dismiss the dialog before finishing.
     */
    class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

        private final List<SavedLocation> mItems;

        ItemAdapter(List<SavedLocation> items) {
            mItems = new java.util.ArrayList<>(items);
        }

        SavedLocation getItem(int position) { return mItems.get(position); }

        void removeItem(int position) {
            mItems.remove(position);
            notifyItemRemoved(position);
        }

        /** Override in anonymous subclasses (e.g. the dialog adapter) if needed. */
        void onLocationSelected(SavedLocation loc) {
            Executors.newSingleThreadExecutor().execute(() -> selectSavedLocation(loc));
            mLocationResolver = new LocationResolver(
                    GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
            mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());

            Runnable finish = () -> {
                mChosenLatLng = new LatLng(sLatitude, sLongitude);
                if (mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(mChosenLatLng).title(sCurrentLocationName));
                    animateMapTo(mChosenLatLng);
                }
                binding.searchRV.setVisibility(View.GONE);
                showSnackbar(getString(R.string.the_application_will_not_track_your_location),
                        Color.RED, Color.WHITE);
            };

            if (mSharedPreferences.getBoolean("useElevation", true)
                    && !mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
                new Thread(() -> mLocationResolver.getElevationFromWebService(
                        new Handler(getMainLooper()), null, finish)).start();
            } else {
                finish.run();
            }
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_text, parent, false);
            return new ItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            SavedLocation loc = mItems.get(position);
            if (loc.name == null || loc.name.isEmpty()) {
                holder.itemView.setVisibility(View.GONE);
                return;
            }
            holder.setIsRecyclable(false);
            if (holder.textView != null) {
                holder.textView.setText(loc.name);
            }
            holder.itemView.setOnClickListener(v -> onLocationSelected(loc));
            holder.itemView.setOnLongClickListener(v -> {
                askDeleteConfirmation(loc, position);
                return true;
            });
        }

        @Override public int getItemCount() { return mItems.size(); }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ItemViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                if (textView != null) textView.setTextIsSelectable(false);
            }
        }
    }

    private void askDeleteConfirmation(SavedLocation loc, int adapterPosition) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(loc.name)
                .setMessage(R.string.do_you_want_to_delete_this_location)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() ->
                            mLocationDao.deleteById(loc.id));
                    if (mItemAdapter != null) {
                        mItemAdapter.removeItem(adapterPosition);
                    }
                })
                .setNegativeButton(R.string.no, (d, w) -> d.dismiss())
                .show();
    }
}
