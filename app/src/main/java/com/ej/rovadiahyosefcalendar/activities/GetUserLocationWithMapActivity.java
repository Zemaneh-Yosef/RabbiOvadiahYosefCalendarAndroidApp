package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sElevation;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sLongitude;

import android.content.DialogInterface;
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
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class GetUserLocationWithMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityGetUserLocationWithMapBinding binding;
    private SharedPreferences mSharedPreferences;
    private LatLng chosenLocation;
    private List<String> mLocationList;
    private ItemAdapter itemAdapter;
    private Marker currentLocation;
    private String bLocationName;
    private double bLat;
    private double bLong;
    private String bTimezoneID;
    private String bZipcode;
    private boolean bUseZipcode;
    private boolean bUseAdvanced;
    private boolean bUseLocation1;
    private boolean bUseLocation2;
    private boolean bUseLocation3;
    private boolean bUseLocation4;
    private boolean bUseLocation5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationResolver.getTimeshapeEngine();// need to init as soon as possible

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
                createZipcodeDialog();
                return true;
            } else if (id== R.id.advanced) {
                createAdvancedDialog();
            } else if (id == R.id.skip) {
                finish();
            }
            return false;
        });

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        // Backup old location details if the user goes back without finishing
        bLocationName = sCurrentLocationName;
        bLat = sLatitude;
        bLong = sLongitude;
        bTimezoneID = sCurrentTimeZoneID;
        bZipcode = mSharedPreferences.getString("Zipcode", "");
        bUseZipcode = mSharedPreferences.getBoolean("useZipcode", false);
        bUseAdvanced = mSharedPreferences.getBoolean("useAdvanced", false);
        bUseLocation1 = mSharedPreferences.getBoolean("useLocation1", false);
        bUseLocation2 = mSharedPreferences.getBoolean("useLocation2", false);
        bUseLocation3 = mSharedPreferences.getBoolean("useLocation3", false);
        bUseLocation4 = mSharedPreferences.getBoolean("useLocation4", false);
        bUseLocation5 = mSharedPreferences.getBoolean("useLocation5", false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.deviceLocation.setOnClickListener(v -> {
            if (mMap != null) {
                if (currentLocation != null) {
                    currentLocation.remove();
                    currentLocation = null;
                }

                setUseLocations(false, false, false, false, false);
                mSharedPreferences.edit()
                        .putBoolean("useAdvanced", false)
                        .putBoolean("useZipcode", false)
                        .apply();

                LocationResolver locationResolver = new LocationResolver(this, this);
                locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
                Thread thread = new Thread(() -> {
                    while (sLatitude == 0 && sLongitude == 0) {
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    runOnUiThread(() -> {
                        chosenLocation = new LatLng(sLatitude, sLongitude);
                        locationResolver.getFullLocationName(true, locationName -> {
                            if (locationName != null) {
                                runOnUiThread(() -> currentLocation = mMap.addMarker(new MarkerOptions().position(chosenLocation).draggable(true).title(locationName)));
                            }
                        });
                        LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                        LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                        Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.the_application_will_keep_requesting_your_location), Snackbar.LENGTH_SHORT)
                                .setBackgroundTint(getColor(R.color.green))
                                .setTextColor(getColor(R.color.black))
                                .show();
                    });
                });
                thread.start();
            }
        });

        mLocationList = new ArrayList<>();
        mLocationList.add(mSharedPreferences.getString("location1", ""));
        mLocationList.add(mSharedPreferences.getString("location2", ""));
        mLocationList.add(mSharedPreferences.getString("location3", ""));
        mLocationList.add(mSharedPreferences.getString("location4", ""));
        mLocationList.add(mSharedPreferences.getString("location5", ""));

        binding.searchRV.setLayoutManager(new LinearLayoutManager(GetUserLocationWithMapActivity.this));
        binding.searchRV.addItemDecoration(new DividerItemDecoration(GetUserLocationWithMapActivity.this, DividerItemDecoration.VERTICAL));
        itemAdapter = new ItemAdapter(mLocationList);
        binding.searchRV.setAdapter(itemAdapter);

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (currentLocation != null) {
                    currentLocation.remove();
                    currentLocation = null;
                }
                setUseLocations(false, false, false, false, false);
                mSharedPreferences.edit().putBoolean("useAdvanced", false)
                        .putBoolean("useZipcode", true)
                        .putString("Zipcode", query)
                        .apply();
                binding.searchView.clearFocus();
                LocationResolver mLocationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                mLocationResolver.setTimeZoneID();
                chosenLocation = new LatLng(sLatitude, sLongitude);
                if (mMap != null) {
                    mLocationResolver.getFullLocationName(true, locationName -> {
                        if (locationName != null) {
                            runOnUiThread(() -> currentLocation = mMap.addMarker(new MarkerOptions().position(chosenLocation).draggable(true).title(locationName)));
                        }
                    });
                    LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                    LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                }
                Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.the_application_will_not_track_your_location), Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.RED)
                        .show();
                updateRV("");
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateRV(newText);
                //TODO show suggestions from an external API, maybe Geonames. But for now, we are using it just for the old locations
                return false;
            }
        });

        binding.confirmLocation.setOnClickListener(v -> {
            if (chosenLocation != null) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.confirmLocation.setEnabled(false);
                LocationResolver locationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                Runnable finish = () -> {
                    if (mSharedPreferences.getBoolean("useAdvanced", false)) {
                        locationResolver.acquireTimeZoneID();
                    } else {// using regular location services, or zipcode
                        locationResolver.setTimeZoneID();
                    }
                    configureSettingsBasedOnLocation();
                    finish();
                };
                if (mSharedPreferences.getBoolean("useElevation", true)) {
                    if (mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
                        finish.run();
                    } else {
                        Thread thread = new Thread(() -> locationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, finish));
                        thread.start();
                    }
                } else {
                    finish.run();
                }
            } else {
                Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.no_location_has_been_set), Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(Color.RED)
                        .show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.confirmLocation, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sCurrentLocationName = bLocationName;
                sLatitude = bLat;
                sLongitude = bLong;
                sCurrentTimeZoneID = bTimezoneID;
                mSharedPreferences.edit().putString("Zipcode", bZipcode)
                        .putBoolean("useZipcode", bUseZipcode)
                        .putBoolean("useAdvanced", bUseAdvanced)
                        .putBoolean("useLocation1", bUseLocation1)
                        .putBoolean("useLocation2", bUseLocation2)
                        .putBoolean("useLocation3", bUseLocation3)
                        .putBoolean("useLocation4", bUseLocation4)
                        .putBoolean("useLocation5", bUseLocation5).apply();
                finish();
                if (!getIntent().getBooleanExtra("loneActivity", false)) {
                    startActivity(new Intent(getApplicationContext(), WelcomeScreenActivity.class));
                }
            }
        });
    }

    @Override
    protected void onStart() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
        super.onStart();
    }

    private void configureSettingsBasedOnLocation() {
        if (Utils.isInOrNearIsrael(sLatitude, sLongitude)) {
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                startActivity(new Intent(this, InIsraelActivity.class));
            }
        } else if (!Utils.isLocaleHebrew(this)) {
            mSharedPreferences.edit().putBoolean("LuachAmudeiHoraah", true).apply();
            mSharedPreferences.edit().putBoolean("useElevation", false).apply();
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
                startActivity(new Intent(this, ZmanimLanguageActivity.class));
            }
        } else {// user is outside of Israel and device is in hebrew
            mSharedPreferences.edit().putBoolean("useElevation", false).apply();
            if (!getIntent().getBooleanExtra("loneActivity", false)) {
                mSharedPreferences.edit().putBoolean("inIsrael", false).apply();
            }
            mSharedPreferences.edit().putBoolean("LuachAmudeiHoraah", true).apply();
            mSharedPreferences.edit().putBoolean("isZmanimInHebrew", true).apply();
            mSharedPreferences.edit().putBoolean("isZmanimEnglishTranslated", false).apply();
            mSharedPreferences.edit().putBoolean("isSetup", true).apply();
            if (mSharedPreferences.getBoolean("hasNotShownTipScreen", true)) {
                startActivity(new Intent(getBaseContext(), TipScreenActivity.class));
                mSharedPreferences.edit().putBoolean("hasNotShownTipScreen", false).apply();
            }
        }
    }

    private void updateRV(String newText) {
        if (newText.isEmpty()) {
            binding.searchRV.setVisibility(View.GONE);
        } else {
            mLocationList = new ArrayList<>();
            mLocationList.add(mSharedPreferences.getString("location1", ""));
            mLocationList.add(mSharedPreferences.getString("location2", ""));
            mLocationList.add(mSharedPreferences.getString("location3", ""));
            mLocationList.add(mSharedPreferences.getString("location4", ""));
            mLocationList.add(mSharedPreferences.getString("location5", ""));
            if (mLocationList.stream().allMatch(String::isEmpty)) {// if no previous locations
                binding.searchRV.setVisibility(View.GONE);
            } else {
                itemAdapter = new ItemAdapter(mLocationList);
                ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(
                        0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false; // Do not support drag-and-drop
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAbsoluteAdapterPosition();
                        mLocationList.remove(position);
                        mSharedPreferences.edit().putString("location" + (position + 1), "").apply();
                        itemAdapter.notifyItemRemoved(position);
                        Snackbar.make(GetUserLocationWithMapActivity.this, viewHolder.itemView, getString(R.string.location_deleted), Snackbar.LENGTH_SHORT).show();
                    }
                };
                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
                itemTouchHelper.attachToRecyclerView(binding.searchRV);
                binding.searchRV.setAdapter(itemAdapter);
                binding.searchRV.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (getWindowManager() != null) {
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            mMap.setPadding(width / 10, height / 10, width / 10, height / 10);
        }

        // Add a marker in the current location and move the camera
        if (sLatitude != 0 && sLongitude != 0) {
            LatLng current = new LatLng(sLatitude, sLongitude);
            currentLocation = mMap.addMarker(new MarkerOptions().position(current).draggable(true).title(sCurrentLocationName));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
            LatLng northEastCorner = SphericalUtil.computeOffset(current, 950000.0 / 100, 45.0);
            LatLng southWestCorner = SphericalUtil.computeOffset(current, 950000.0 / 100, 225.0);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
        }

        mMap.setOnMapClickListener(latLng -> {
            if (currentLocation != null) {
                currentLocation.remove();
                currentLocation = null;
            }
            chosenLocation = latLng;

            setUseLocations(false, false, false, false, false);
            mSharedPreferences.edit()
                    .putBoolean("useAdvanced", true)
                    .putBoolean("useZipcode", false)
                    .apply();

            sLatitude = latLng.latitude;
            sLongitude = latLng.longitude;
            LocationResolver locationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);

            locationResolver.getFullLocationName(true, locationName -> {
                if (locationName != null) {
                    runOnUiThread(() -> currentLocation = mMap.addMarker(new MarkerOptions().position(latLng).draggable(true).title(locationName)));
                    sCurrentLocationName = locationName;

                    mSharedPreferences.edit()
                            .putString("advancedLN", sCurrentLocationName)
                            .putString("advancedLat", String.valueOf(sLatitude))
                            .putString("advancedLong", String.valueOf(sLongitude)).apply();
                }
            });
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.the_application_will_not_track_your_location), Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.RED)
                    .show();
        });
    }

    private void createZipcodeDialog() {
        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);

        Button locationOne = new Button(this);
        locationOne.setText(mSharedPreferences.getString("location1", ""));
        if (locationOne.getText().equals("")) {
            locationOne.setVisibility(View.GONE);
        }
        if (locationOne.getVisibility() != View.GONE) {
            locationOne.setOnLongClickListener(view -> {
                askUserIfTheyWantToDeleteThisEntry("location1", locationOne);
                return false;
            });
        }

        Button locationTwo = new Button(this);
        locationTwo.setText(mSharedPreferences.getString("location2", ""));
        if (locationTwo.getText().equals("")) {
            locationTwo.setVisibility(View.GONE);
        }
        if (locationTwo.getVisibility() != View.GONE) {
            locationTwo.setOnLongClickListener(view -> {
                askUserIfTheyWantToDeleteThisEntry("location2", locationTwo);
                return false;
            });
        }

        Button locationThree = new Button(this);
        locationThree.setText(mSharedPreferences.getString("location3", ""));
        if (locationThree.getText().equals("")) {
            locationThree.setVisibility(View.GONE);
        }
        if (locationThree.getVisibility() != View.GONE) {
            locationThree.setOnLongClickListener(view -> {
                askUserIfTheyWantToDeleteThisEntry("location3", locationThree);
                return false;
            });
        }

        Button locationFour = new Button(this);
        locationFour.setText(mSharedPreferences.getString("location4", ""));
        if (locationFour.getText().equals("")) {
            locationFour.setVisibility(View.GONE);
        }
        if (locationFour.getVisibility() != View.GONE) {
            locationFour.setOnLongClickListener(view -> {
                askUserIfTheyWantToDeleteThisEntry("location4", locationFour);
                return false;
            });
        }

        Button locationFive = new Button(this);
        locationFive.setText(mSharedPreferences.getString("location5", ""));
        if (locationFive.getText().equals("")) {
            locationFive.setVisibility(View.GONE);
        }
        if (locationFive.getVisibility() != View.GONE) {
            locationFive.setOnLongClickListener(view -> {
                askUserIfTheyWantToDeleteThisEntry("location5", locationFive);
                return false;
            });
        }

        linearLayout.addView(locationOne);
        linearLayout.addView(locationTwo);
        linearLayout.addView(locationThree);
        linearLayout.addView(locationFour);
        linearLayout.addView(locationFive);

        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint(R.string.enter_zipcode_or_address);
        input.setSingleLine();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        linearLayout.addView(input);

        alertDialog.setTitle(R.string.search_for_a_place)
                .setMessage(R.string.warning_zmanim_will_be_based_on_your_approximate_area)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        createZipcodeDialog();
                    } else {
                        setUseLocations(false, false, false, false, false);
                        mSharedPreferences.edit()
                                .putBoolean("useAdvanced", false)
                                .putBoolean("useZipcode", true)
                                .putString("Zipcode", input.getText().toString())
                                .apply();
                        LocationResolver mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                        mLocationResolver.setTimeZoneID();
                        Runnable finish = () -> {
                            configureSettingsBasedOnLocation();
                            finish();
                        };
                        if (mSharedPreferences.getBoolean("useElevation", true)) {
                            if (mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
                                finish.run();
                            } else {
                                Thread thread = new Thread(() ->
                                        mLocationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, finish));
                                thread.start();
                            }
                        } else {
                           finish.run();
                        }
                    }
                })
                .setNegativeButton(R.string.advanced, (dialog, which) -> createAdvancedDialog())
                .setNeutralButton(R.string.use_location, (dialog, which) -> {
                    setUseLocations(false, false, false, false, false);
                    mSharedPreferences.edit()
                            .putBoolean("useAdvanced", false)
                            .putBoolean("useZipcode", false)
                            .apply();
                    LocationResolver mLocationResolver = new LocationResolver(this, this);
                    mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
                    mLocationResolver.setTimeZoneID();
                    Runnable finish = () -> {
                        configureSettingsBasedOnLocation();
                        finish();
                    };
                    if (mSharedPreferences.getBoolean("useElevation", true)) {
                        if (mSharedPreferences.contains("elevation" + sCurrentLocationName)) {
                            finish.run();
                        } else {
                            Thread thread = new Thread(() ->
                                    mLocationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, finish));
                            thread.start();
                        }
                    } else {
                        finish.run();
                    }
                });

        AlertDialog ad = alertDialog.create();
        ad.show();

        input.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                ad.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });

        locationOne.setOnClickListener(view -> {
            setUseLocations(true, false, false, false, false);
            ad.dismiss();
            configureSettingsBasedOnLocation();
            finish();
        });

        locationTwo.setOnClickListener(view -> {
            setUseLocations(false, true, false, false, false);
            ad.dismiss();
            configureSettingsBasedOnLocation();
            finish();
        });

        locationThree.setOnClickListener(view -> {
            setUseLocations(false, false, true, false, false);
            ad.dismiss();
            configureSettingsBasedOnLocation();
            finish();
        });

        locationFour.setOnClickListener(view -> {
            setUseLocations(false, false, false, true, false);
            ad.dismiss();
            configureSettingsBasedOnLocation();
            finish();
        });

        locationFive.setOnClickListener(view -> {
            setUseLocations(false, false, false, false, true);
            ad.dismiss();
            configureSettingsBasedOnLocation();
            finish();
        });
    }

    private void createAdvancedDialog() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);

        TextView locationName = new TextView(this);
        locationName.setText(R.string.enter_location_name);
        locationName.setGravity(Gravity.CENTER);

        EditText locationInput = new EditText(this);
        locationInput.setText(sCurrentLocationName);
        locationInput.setHint(R.string.location_hint);
        locationInput.setGravity(Gravity.CENTER);

        TextView latitude = new TextView(this);
        latitude.setText(R.string.enter_latitude);
        latitude.setGravity(Gravity.CENTER);

        EditText latInput = new EditText(this);
        latInput.setText(String.valueOf(sLatitude));
        latInput.setHint("ex: 73.09876543");
        latInput.setGravity(Gravity.CENTER);

        TextView longitude = new TextView(this);
        longitude.setText(R.string.enter_longitude);
        longitude.setGravity(Gravity.CENTER);

        EditText longInput = new EditText(this);
        longInput.setText(String.valueOf(sLongitude));
        longInput.setHint("ex: -103.098765");
        longInput.setGravity(Gravity.CENTER);

        TextView elevation = new TextView(this);
        elevation.setText(R.string.enter_elevation_in_meters);
        elevation.setGravity(Gravity.CENTER);

        EditText elevationInput = new EditText(this);
        elevationInput.setText(String.valueOf(sElevation));
        elevationInput.setHint("ex: 805");
        elevationInput.setGravity(Gravity.CENTER);

        TextView timezone = new TextView(this);
        timezone.setText(R.string.choose_timezone);
        timezone.setGravity(Gravity.CENTER);

        Spinner timezones = new Spinner(this);
        timezones.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TimeZone.getAvailableIDs()));
        timezones.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String s = (String) parent.getItemAtPosition(position);
                mSharedPreferences.edit().putString("advancedTimezone", s).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        linearLayout.addView(locationName);
        linearLayout.addView(locationInput);

        linearLayout.addView(latitude);
        linearLayout.addView(latInput);

        linearLayout.addView(longitude);
        linearLayout.addView(longInput);

        linearLayout.addView(elevation);
        linearLayout.addView(elevationInput);

        linearLayout.addView(timezone);
        linearLayout.addView(timezones);

        scrollView.addView(linearLayout);

        MaterialAlertDialogBuilder advancedAlert = new MaterialAlertDialogBuilder(this);

        advancedAlert.setTitle(R.string.advanced)
                .setView(scrollView)
                .setPositiveButton(R.string.ok, (dialogAd, whichAd) -> {
                    if (locationInput.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        createZipcodeDialog();
                    } else {
                        setUseLocations(false, false, false, false, false);
                        mSharedPreferences.edit()
                                .putBoolean("useAdvanced", true)
                                .putString("advancedLN", locationInput.getText().toString())
                                .putString("advancedLat", latInput.getText().toString())
                                .putString("advancedLong", longInput.getText().toString())
                                .putString("elevation" + locationInput.getText().toString(),
                                elevationInput.getText().toString()).apply();

                        LocationResolver mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
                        configureSettingsBasedOnLocation();
                        finish();
                    }
                });
        advancedAlert.show();
    }

    private void askUserIfTheyWantToDeleteThisEntry(String location, Button locationButton) {
        String locationName = mSharedPreferences.getString(location, "");
        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(this);
        alertDialog.setTitle(locationName)
                .setMessage(R.string.do_you_want_to_delete_this_location)
                .setPositiveButton(getString(R.string.yes), (dialogInterface, i) -> {
                    mSharedPreferences.edit().putString(location, "").apply();
                    locationButton.setVisibility(View.GONE);
                })
                .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private void setUseLocations(boolean location1, boolean location2, boolean location3, boolean location4, boolean location5) {
        mSharedPreferences.edit().putBoolean("useLocation1", location1)
                .putBoolean("useLocation2", location2)
                .putBoolean("useLocation3", location3)
                .putBoolean("useLocation4", location4)
                .putBoolean("useLocation5", location5)
                .apply();
    }

    class ItemAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        private final List<String> itemList;
        public ItemAdapter(List<String> itemList) {
            this.itemList = itemList;
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view, parent, false);
            return new ItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            String item = itemList.get(position);
            if (item.isEmpty() && holder.itemTextView != null) {
                holder.itemTextView.setVisibility(View.GONE);
            }
            holder.setIsRecyclable(false);
            if (holder.itemTextView != null) {
                holder.itemTextView.setText(item);
            }

            holder.itemView.setOnClickListener(v -> {
                mSharedPreferences.edit()
                        .putBoolean("useAdvanced", false)
                        .putBoolean("useZipcode", false).apply();

                boolean location1 = false;
                boolean location2 = false;
                boolean location3 = false;
                boolean location4 = false;
                boolean location5 = false;

                if (item.equals(mSharedPreferences.getString("location1", ""))) {
                    location1 = true;
                } else if (item.equals(mSharedPreferences.getString("location2", ""))) {
                    location2 = true;
                } else if (item.equals(mSharedPreferences.getString("location3", ""))) {
                    location3 = true;
                } else if (item.equals(mSharedPreferences.getString("location4", ""))) {
                    location4 = true;
                } else if (item.equals(mSharedPreferences.getString("location5", ""))) {
                    location5 = true;
                }

                mSharedPreferences.edit().putBoolean("useLocation1", location1)
                        .putBoolean("useLocation2", location2)
                        .putBoolean("useLocation3", location3)
                        .putBoolean("useLocation4", location4)
                        .putBoolean("useLocation5", location5)
                        .apply();
                LocationResolver mLocationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                mLocationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
                mLocationResolver.setTimeZoneID();
                Runnable finish = () -> {
                    chosenLocation = new LatLng(sLatitude, sLongitude);
                    mMap.addMarker(new MarkerOptions().position(chosenLocation).title(sCurrentLocationName));
                    LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                    LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                    binding.searchRV.setVisibility(View.GONE);
                    Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.the_application_will_not_track_your_location), Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(Color.RED)
                            .show();

                };
                if (mSharedPreferences.getBoolean("useElevation", true)) {
                    if (mSharedPreferences.contains("elevation" + sCurrentLocationName)) {// we already have the elevation data
                        finish.run();
                    } else {
                    Thread thread = new Thread(() ->
                            mLocationResolver.getElevationFromWebService(new Handler(getMainLooper()), null, finish));
                    thread.start();
                    }
                } else {
                    finish.run();
                }
            });
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView itemTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemTextView = itemView.findViewById(R.id.textView);
            if (itemTextView != null) {
                itemTextView.setTextIsSelectable(false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (currentLocation != null) {
                currentLocation.remove();
                currentLocation = null;
            }
            LocationResolver locationResolver = new LocationResolver(this, this);
            locationResolver.acquireLatitudeAndLongitude(new ZmanimFragment());
            Thread thread = new Thread(() -> {
                while (sLatitude == 0 && sLongitude == 0) {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                runOnUiThread(() -> {
                    chosenLocation = new LatLng(sLatitude, sLongitude);
                    locationResolver.getFullLocationName(true, locationName -> {
                        if (locationName != null) {
                            runOnUiThread(() -> currentLocation = mMap.addMarker(new MarkerOptions().position(chosenLocation).draggable(true).title(locationName)));
                        }
                    });
                    LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                    LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                    Snackbar.make(GetUserLocationWithMapActivity.this, binding.getRoot(), getString(R.string.the_application_will_keep_requesting_your_location), Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(getColor(R.color.green))
                            .setTextColor(getColor(R.color.black))
                            .show();
                });
            });
            thread.start();
        }
    }
}

