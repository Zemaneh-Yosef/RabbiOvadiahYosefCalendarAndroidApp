package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainActivity.sLongitude;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.LocationResolver;
import com.ej.rovadiahyosefcalendar.databinding.ActivityGetUserLocationWithMapBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.SphericalUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

        binding = ActivityGetUserLocationWithMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setActionBar(binding.toolbar);
        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);

        mSharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);

        // Backup old location details if the user goes back without finishing
        bLocationName = MainActivity.sCurrentLocationName;
        bLat = sLatitude;
        bLong = sLongitude;
        bTimezoneID = MainActivity.sCurrentTimeZoneID;
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

        binding.fab.setOnClickListener(v -> {
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
                locationResolver.acquireLatitudeAndLongitude();
                if (sLatitude != 0 && sLongitude != 0) {
                    chosenLocation = new LatLng(sLatitude, sLongitude);
                    currentLocation = mMap.addMarker(new MarkerOptions().position(chosenLocation).draggable(true).title(locationResolver.getFullLocationName()));
                    LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                    LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                }
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
                LocationResolver mLocationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                chosenLocation = new LatLng(sLatitude, sLongitude);
                currentLocation = mMap.addMarker(new MarkerOptions().position(chosenLocation).draggable(true).title(mLocationResolver.getFullLocationName()));
                LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
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
                binding.confirmLocation.setEnabled(false);
                LocationResolver locationResolver = new LocationResolver(GetUserLocationWithMapActivity.this, GetUserLocationWithMapActivity.this);
                if (mSharedPreferences.getBoolean("useElevation", true)) {
                    locationResolver.start();
                    try {
                        locationResolver.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (mSharedPreferences.getBoolean("useAdvanced", false)) {
                    locationResolver.aquireTimeZoneID();
                } else {// using regular location services, or zipcode
                    locationResolver.setTimeZoneID();
                }
                if (!mSharedPreferences.getBoolean("inIsrael", false) && !mSharedPreferences.getBoolean("isSetup", false)) {
                    startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                }
                mSharedPreferences.edit().putBoolean("isSetup", true).apply();
                finish();
            }
        });
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
            currentLocation = mMap.addMarker(new MarkerOptions().position(current).draggable(true).title(MainActivity.sCurrentLocationName));
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
            MainActivity.sCurrentLocationName = locationResolver.getLocationName();

            mSharedPreferences.edit()
                    .putString("advancedLN", MainActivity.sCurrentLocationName)
                    .putString("advancedLat", String.valueOf(sLatitude))
                    .putString("advancedLong", String.valueOf(sLongitude)).apply();

            currentLocation = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(locationResolver.getFullLocationName()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        });
    }

    private void createZipcodeDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.alertDialog);

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
                    boolean isSetup = mSharedPreferences.getBoolean("isSetup", false);
                    if (input.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        createZipcodeDialog();
                    } else {
                        setUseLocations(false, false, false, false, false);
                        mSharedPreferences.edit().putBoolean("isSetup", true)
                                .putBoolean("useAdvanced", false)
                                .putBoolean("useZipcode", true)
                                .putString("Zipcode", input.getText().toString())
                                .apply();
                        LocationResolver mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.getLatitudeAndLongitudeFromSearchQuery();
                        mLocationResolver.setTimeZoneID();
                        if (mSharedPreferences.getBoolean("useElevation", true)) {
                            mLocationResolver.start();
                            try {
                                mLocationResolver.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!mSharedPreferences.getBoolean("inIsrael", false) && !isSetup) {
                            startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        }
                        finish();
                    }
                })
                .setNegativeButton(R.string.advanced, (dialog, which) -> {
                    createAdvancedDialog();
                })
                .setNeutralButton(R.string.use_location, (dialog, which) -> {
                    boolean isSetup = mSharedPreferences.getBoolean("isSetup", false);
                    setUseLocations(false, false, false, false, false);
                    mSharedPreferences.edit().putBoolean("isSetup", true)
                            .putBoolean("useAdvanced", false)
                            .putBoolean("useZipcode", false)
                            .apply();
                    LocationResolver mLocationResolver = new LocationResolver(this, this);
                    mLocationResolver.acquireLatitudeAndLongitude();
                    mLocationResolver.setTimeZoneID();
                    if (mSharedPreferences.getBoolean("useElevation", true)) {
                        mLocationResolver.start();
                        try {
                            mLocationResolver.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!mSharedPreferences.getBoolean("inIsrael", false) && !isSetup) {
                        startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                    }
                    finish();
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
            finish();
        });

        locationTwo.setOnClickListener(view -> {
            setUseLocations(false, true, false, false, false);
            ad.dismiss();
            finish();
        });

        locationThree.setOnClickListener(view -> {
            setUseLocations(false, false, true, false, false);
            ad.dismiss();
            finish();
        });

        locationFour.setOnClickListener(view -> {
            setUseLocations(false, false, false, true, false);
            ad.dismiss();
            finish();
        });

        locationFive.setOnClickListener(view -> {
            setUseLocations(false, false, false, false, true);
            ad.dismiss();
            finish();
        });
    }

    private void createAdvancedDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView locationName = new TextView(this);
        locationName.setText(R.string.enter_location_name);
        locationName.setGravity(Gravity.CENTER);

        EditText locationInput = new EditText(this);
        locationInput.setHint(R.string.location_hint);
        locationInput.setGravity(Gravity.CENTER);

        TextView latitude = new TextView(this);
        latitude.setText(R.string.enter_latitude);
        latitude.setGravity(Gravity.CENTER);

        EditText latInput = new EditText(this);
        latInput.setHint("ex: 73.09876543");
        latInput.setGravity(Gravity.CENTER);

        TextView longitude = new TextView(this);
        longitude.setText(R.string.enter_longitude);
        longitude.setGravity(Gravity.CENTER);

        EditText longInput = new EditText(this);
        longInput.setHint("ex: -103.098765");
        longInput.setGravity(Gravity.CENTER);

        TextView elevation = new TextView(this);
        elevation.setText(R.string.enter_elevation_in_meters);
        elevation.setGravity(Gravity.CENTER);

        EditText elevationInput = new EditText(this);
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        layout.addView(locationName);
        layout.addView(locationInput);

        layout.addView(latitude);
        layout.addView(latInput);

        layout.addView(longitude);
        layout.addView(longInput);

        layout.addView(elevation);
        layout.addView(elevationInput);

        layout.addView(timezone);
        layout.addView(timezones);

        AlertDialog.Builder advancedAlert = new AlertDialog.Builder(this, R.style.alertDialog);

        advancedAlert.setTitle(R.string.advanced)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialogAd, whichAd) -> {
                    boolean isSetup = mSharedPreferences.getBoolean("isSetup", false);
                    if (locationInput.getText().toString().isEmpty()) {
                        Toast.makeText(this, R.string.please_enter_something, Toast.LENGTH_SHORT).show();
                        createZipcodeDialog();
                    } else {
                        setUseLocations(false, false, false, false, false);
                        mSharedPreferences.edit().putBoolean("isSetup", true)
                                .putBoolean("useAdvanced", true)
                                .putString("advancedLN", locationInput.getText().toString())
                                .putString("advancedLat", latInput.getText().toString())
                                .putString("advancedLong", longInput.getText().toString())
                                .putString("elevation" + locationInput.getText().toString(),
                                elevationInput.getText().toString()).apply();

                        LocationResolver mLocationResolver = new LocationResolver(this, this);
                        mLocationResolver.acquireLatitudeAndLongitude();
                        if (!mSharedPreferences.getBoolean("inIsrael", false) && !isSetup) {
                            startActivity(new Intent(this, CalendarChooserActivity.class).setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        }
                        finish();
                    }
                });
        advancedAlert.create().show();
    }

    private void askUserIfTheyWantToDeleteThisEntry(String location, Button locationButton) {
        String locationName = mSharedPreferences.getString("location1", "");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.alertDialog);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_place_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search_for_a_place_legacy) {
            createZipcodeDialog();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.advanced) {
            createAdvancedDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MainActivity.sCurrentLocationName = bLocationName;
        sLatitude = bLat;
        sLongitude = bLong;
        MainActivity.sCurrentTimeZoneID = bTimezoneID;
        mSharedPreferences.edit().putString("Zipcode", bZipcode)
                .putBoolean("useZipcode", bUseZipcode)
                .putBoolean("useAdvanced", bUseAdvanced)
                .putBoolean("useLocation1", bUseLocation1)
                .putBoolean("useLocation2", bUseLocation2)
                .putBoolean("useLocation3", bUseLocation3)
                .putBoolean("useLocation4", bUseLocation4)
                .putBoolean("useLocation5", bUseLocation5).apply();
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
            if (item.isEmpty()) {
                holder.itemTextView.setVisibility(View.GONE);
            }
            holder.setIsRecyclable(false);
            holder.itemTextView.setText(item);

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
                mLocationResolver.acquireLatitudeAndLongitude();
                if (mSharedPreferences.getBoolean("useElevation", true)) {
                    mLocationResolver.start();
                    try {
                        mLocationResolver.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                chosenLocation = new LatLng(sLatitude, sLongitude);
                mMap.addMarker(new MarkerOptions().position(chosenLocation).title(MainActivity.sCurrentLocationName));
                LatLng northEastCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 45.0);
                LatLng southWestCorner = SphericalUtil.computeOffset(chosenLocation, 950000.0 / 100, 225.0);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
                binding.searchRV.setVisibility(View.GONE);
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
            itemTextView.setTextIsSelectable(false);
        }
    }
}

