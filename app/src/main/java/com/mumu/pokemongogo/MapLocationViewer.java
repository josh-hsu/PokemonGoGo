package com.mumu.pokemongogo;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MapLocationViewer extends AppCompatActivity
        implements
        OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "PokemonGoGo";

    private boolean mPermissionDenied = false;
    private static GoogleMap mMap;
    private static LatLng mUserSelectPoint = null;
    private LongPressLocationSource mLocationSource;

    /**
     * A {@link LocationSource} which reports a new location whenever a user long presses the map
     * at the point at which a user long pressed the map.
     */
    private static class LongPressLocationSource implements GoogleMap.OnMapLongClickListener {

        @Override
        public void onMapLongClick(LatLng point) {
            if (point != null) {
                Log.d(TAG, "User hit LAT = " + point.latitude + " and LONG = " + point.longitude);
                mUserSelectPoint = point;
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(mUserSelectPoint).title("Marker"));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_location_viewer);

        mLocationSource = new LongPressLocationSource();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_finish) {
            if (mUserSelectPoint == null) {
                Toast.makeText(this, getString(R.string.msg_map_no_point), Toast.LENGTH_SHORT).show();
            } else {
                final Intent intent = new Intent(this, HeadService.class);
                intent.setAction(HeadService.ACTION_HANDLE_NAVIGATION);
                intent.putExtra(HeadService.EXTRA_DATA, mUserSelectPoint);
                startService(intent);
                finish();
            }
            return true;
        } else if (id == R.id.action_cancel) {
            Toast.makeText(this, getString(R.string.msg_map_cancelled), Toast.LENGTH_SHORT).show();
            finish();
            return true;
        } else if (id == R.id.action_teleport) {
            if (mUserSelectPoint == null) {
                Toast.makeText(this, getString(R.string.msg_map_no_point), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.msg_map_shu), Toast.LENGTH_SHORT).show();
                final Intent intent = new Intent(this, HeadService.class);
                intent.setAction(HeadService.ACTION_HANDLE_TELEPORT);
                intent.putExtra(HeadService.EXTRA_DATA, mUserSelectPoint);
                startService(intent);
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMapLongClickListener(mLocationSource);
        enableMyLocation();
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, getString(R.string.msg_map_locating), Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
}