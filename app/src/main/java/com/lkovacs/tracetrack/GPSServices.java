package com.lkovacs.tracetrack;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

public class GPSServices extends AppCompatActivity implements LocationListener {

    public static final double DEF_LATITUDE = 47.051598;
    public static final double DEF_LONGITUDE = 21.942767;
    protected LocationManager locationManager;
    public LocationListener locationListener;
    Location loc = new Location("GPS_PROVIDER");
    public String locationProvider = LocationManager.GPS_PROVIDER;
    private final Context mContext;
    private static final int REQUEST_GPS_PERMISSION = 1;


    public GPSServices(Context mContext) {
        this.mContext = mContext;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                loc = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
//                showSettingsAlert();
            }
        };
        loc.setLatitude(DEF_LATITUDE);
        loc.setLongitude(DEF_LONGITUDE);
        getLocation();
    }

    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
            loc = locationManager.getLastKnownLocation(locationProvider);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) this.mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this.mContext, R.string.sorryGPS, Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions((Activity) this.mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GPS_PERMISSION);
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions((Activity) this.mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GPS_PERMISSION);
            }
        }
    }

    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean canGetLocation() {
        if (loc == null) {
            return false;
        } else {
            if ((loc.getLatitude() == DEF_LATITUDE) && (loc.getLongitude() == DEF_LONGITUDE)) {
                return false;
            } else {
                return true;
            }
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder gpsAlertDialog = new AlertDialog.Builder(mContext);
        gpsAlertDialog.setTitle(R.string.no_gps_enabled);
        gpsAlertDialog.setMessage(R.string.enab_gps);
        gpsAlertDialog.setCancelable(true);
        gpsAlertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
        gpsAlertDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
        gpsAlertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocation();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}
