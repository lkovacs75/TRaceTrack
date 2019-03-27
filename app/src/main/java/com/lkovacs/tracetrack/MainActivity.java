package com.lkovacs.tracetrack;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public Button start, stop;
    public TextView countView;
    public GPSServices gpsServices;
    public GoogleMap myMap;
    private ProgressDialog pDialog;
    public Location oldLocation = null, newLocation = null;
    public Timer timer = new Timer();
    public int pointCounter;
    public Polyline polyLine;
    private PolylineOptions polylineOptions = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsServices = new GPSServices(this);
        if (gpsServices.loc == null) {
            new waitForGPSSignal().execute("my string parameter");
        }

        pointCounter = 0;
        countView = findViewById(R.id.counterView);
        polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(8).geodesic(true);

        start = findViewById(R.id.startBtn);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start.setClickable(false);
                stop.setClickable(true);
                gpsServices.getLocation();
                oldLocation = gpsServices.loc;
                newLocation = oldLocation;
                TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        gpsServices.getLocation();
                        newLocation = gpsServices.loc;
                        pointCounter++;
                        if (newLocation != oldLocation) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    List<LatLng> points = polyLine.getPoints();
                                    points.add(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
                                    polyLine.setPoints(points);
                                    myMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(newLocation.getLatitude(), newLocation.getLongitude())));
                                    countView.setText("PointCounter: " + String.valueOf(pointCounter) +
                                            "\nLat: " + String.valueOf(newLocation.getLatitude()) +
                                            "\nLng: " + String.valueOf(newLocation.getLongitude()));
                                }
                            });
                            oldLocation = newLocation;
                        }
                    }
                };
                timer = new Timer();
                timer.scheduleAtFixedRate(t, 0, 2000);
            }
        });

        stop = findViewById(R.id.stopBtn);
        stop.setClickable(false);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                start.setClickable(true);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myMap.addMarker(new MarkerOptions()
                                .position(new LatLng(gpsServices.loc.getLatitude(), gpsServices.loc.getLongitude()))
                                .title("test")
                                .snippet("test's snippet")
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.defaultMarker()));
                    }
                });
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(47.160366, 22.044394), 11));
        myMap = googleMap;
        polyLine = myMap.addPolyline(polylineOptions);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class waitForGPSSignal extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.waitForGPS));
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            do {
                gpsServices.getLocation();
            } while (gpsServices.loc == null);
            return "ok";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pDialog.dismiss();
//            currLat.setText(String.valueOf(gpsServices.loc.getLatitude()));
//            currLng.setText(String.valueOf(gpsServices.loc.getLongitude()));
//            currDate.setText(String.valueOf(Calendar.getInstance().getTime()));
        }
    }
}
