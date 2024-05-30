package com.ej.rovadiahyosefcalendar.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.databinding.ActivityJerusalemDirectionMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.maps.android.SphericalUtil;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.ArrayList;
import java.util.LinkedList;

public class JerusalemDirectionMapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final LinkedList<Double> m_window = new LinkedList<>();
    private float smoothedAzimuthDegrees = 0.0f;
    private Marker triMarker;
    private final LatLng jer = new LatLng(31.778015, 35.235413);
    private final LatLng currentLocation = new LatLng(MainActivity.sLatitude, MainActivity.sLongitude);
    private boolean isTriGreen = false;
    private float previousZoomLevel = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityJerusalemDirectionMapsBinding binding = ActivityJerusalemDirectionMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        materialToolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.baseline_arrow_back_24));
        materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.help) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.help))
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .setMessage(R.string.jer_dir_help_text)
                        .show();
                return true;
            } else if (item.getItemId() == android.R.id.home) {
                getOnBackPressedDispatcher().onBackPressed();
            }
            return false;
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map), (v, windowInsets) -> {
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
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (getWindowManager() != null) {
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            googleMap.setPadding(width / 10, height / 10, width / 10, height / 10);
        }

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Add a marker in Jerusalem and move the camera
        googleMap.addMarker(new MarkerOptions().position(jer).title(getString(R.string.holy_of_holies)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(jer));

        Marker markerJerusalem = googleMap.addMarker(
                new MarkerOptions().flat(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .position(jer)
                        .title(getString(R.string.holy_of_holies))
                        .draggable(false)
        );

        if (markerJerusalem != null) {
            markerJerusalem.showInfoWindow();
        }

        googleMap.setOnMapClickListener(
                latLng -> {
                    if (markerJerusalem != null) {
                        markerJerusalem.showInfoWindow();
                    }
                }
        );

        googleMap.setOnMarkerClickListener(marker -> {
            if (markerJerusalem != null) {
                markerJerusalem.showInfoWindow();
            }
            return true;
        });

        Polyline polyline = googleMap.addPolyline(new PolylineOptions().width(4.5f).color(Color.BLUE));
        ArrayList<LatLng> points = new ArrayList<>();
        points.add(new LatLng(MainActivity.sLatitude, MainActivity.sLongitude));
        points.add(jer);
        polyline.setPoints(points);
        //polyline.setGeodesic(true);// If we want to add the Great Circle method

        triMarker = googleMap.addMarker(
                new MarkerOptions().flat(false)
                        .position(new LatLng(0.0, 0.0))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.tri))
                        .draggable(false));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        if (triMarker != null) {
            triMarker.setPosition(currentLocation);
        }

        double distanceInMeters = SphericalUtil.computeDistanceBetween(currentLocation, jer);

        LatLng northEastCorner = SphericalUtil.computeOffset(currentLocation, 0.0, 45.0);
        LatLng southWestCorner = SphericalUtil.computeOffset(currentLocation, 0.0, 225.0);
        LatLngBounds bounds = new LatLngBounds(southWestCorner, northEastCorner);
        bounds = bounds.including(jer);

        if (distanceInMeters > 9500000.0 / 2) {
            northEastCorner = SphericalUtil.computeOffset(currentLocation, 9500000.0 / 100, 45.0);
            southWestCorner = SphericalUtil.computeOffset(currentLocation, 9500000.0 / 100, 225.0);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southWestCorner, northEastCorner), 0));
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
        }

        googleMap.setOnCameraIdleListener(() -> {
            float currentZoomLevel = googleMap.getCameraPosition().zoom;
            // Check if zoom level has changed
            if (previousZoomLevel != -1 && currentZoomLevel != previousZoomLevel) {
                // Zoom event has occurred, move to current location
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
            }
            // Update the previous zoom level
            previousZoomLevel = currentZoomLevel;
        });
    }

    private float filtrate(Double value) {//Credit to Rafael Sheink
        m_window.add(value);
        if (m_window.size() > 50) {
            m_window.remove();
        }

        double sumx = 0.0;
        double sumy = 0.0;
        Object[] arr = m_window.toArray();
        for (Object anArr : arr) {
            if (anArr instanceof Double) {
                sumx += Math.cos((Double) anArr / 360 * (2 * Math.PI));
                sumy += Math.sin((Double) anArr / 360 * (2 * Math.PI));
            }
        }

        double avgx = sumx / m_window.size();
        double avgy = sumy / m_window.size();

        double temp = Math.atan2(avgy, avgx) / (2 * Math.PI) * 360;
        if (temp == 0.0) {
            return 0.0f;
        }

        if (temp > 0) {
            return (float) temp;
        } else {
            return (((float) temp) + 360) % 360;
        }
    }

    private void updateOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            smoothedAzimuthDegrees = filtrate(orientationAngles[0] / (2 * Math.PI) * 360);

            if (triMarker != null) {
                triMarker.setRotation(smoothedAzimuthDegrees);
                checkIfTriangleShouldBeGreen();
            }
        }
    }

    private void checkIfTriangleShouldBeGreen() {
        GeoLocation current = new GeoLocation("", currentLocation.latitude, currentLocation.longitude, null);//Timezone does not matter
        GeoLocation jerusalemLocation = new GeoLocation("", jer.latitude, jer.longitude, null);

        double bearing = current.getRhumbLineBearing(jerusalemLocation);// Specifically use the Rhumb Line method as instructed by Rav Elbaz

        double directionDifference = Math.abs((smoothedAzimuthDegrees - bearing));

        double threshold = 10.0; // Adjust this threshold as needed

        if (directionDifference <= threshold) {
            if (!isTriGreen) {
                triMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.trigreen));
                isTriGreen = true;
            }
        } else {
            if (isTriGreen) {
                triMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.tri));
                isTriGreen = false;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }

        updateOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}