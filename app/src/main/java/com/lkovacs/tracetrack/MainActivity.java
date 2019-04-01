package com.lkovacs.tracetrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public Button start, stop;
    public TextView coordView, avgSpeedView, distanceView, climbView, curSpeedView;
    public GPSServices gpsServices;
    public GoogleMap myMap;
    private ProgressDialog pDialog;
    public Location oldLocation = null, newLocation = null;
    public Timer timer = new Timer();
    public int pointCounter;
    public int distance = 0, climb = 0, GPS_TIME_INTERVAL = 2000;
    public String CHANNEL_ID = "TraceTrack_channel";
    double avgSpeed = 0, curSpeed = 0;
    public Polyline polyLine;
    private PolylineOptions polylineOptions = null;
    public List<LatLng> points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.gps_track_on)
                .setContentTitle("GPS Track ON!")
                .setContentText("Trace tracking working...")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsServices = new GPSServices(this);
        if (gpsServices.loc == null) {
            new waitForGPSSignal().execute("my string parameter");
        }

        coordView = findViewById(R.id.counterView);
        coordView.setText("Latitude: 0.00000\nLongitude: 0.00000");
        avgSpeedView = findViewById(R.id.avgSpeed);
        avgSpeedView.setText("Avg. avgSpeed: 0 km/h.");
        distanceView = findViewById(R.id.distance);
        distanceView.setText("Distance travelled: 0 m.");
        climbView = findViewById(R.id.climb);
        climbView.setText("Elevation change: 0 m.");
        curSpeedView = findViewById(R.id.currSpeedView);
        curSpeedView.setText("Curr.speed: 0 km/h.");
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
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                notificationManager.notify(17, builder.build());
                distance = 0;
                points = polyLine.getPoints();
                points.clear();
                pointCounter = 0;
                TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        gpsServices.getLocation();
                        newLocation = gpsServices.loc;
                        pointCounter++;
                        distance += newLocation.distanceTo(oldLocation);
                        avgSpeed = ((float) distance / (pointCounter * GPS_TIME_INTERVAL)) * 3600;
                        curSpeed = (newLocation.distanceTo(oldLocation) / GPS_TIME_INTERVAL) * 3600;
                        Log.i("===============", "--------------------");
                        Log.i("OldAlt ", String.valueOf(oldLocation.getAltitude()) + " newAlt: " + String.valueOf(newLocation.getAltitude()));
                        if (newLocation != oldLocation) {
                            if (newLocation.getAltitude() > oldLocation.getAltitude()) {
                                Log.i("Elev.change ", String.valueOf(newLocation.getAltitude() - oldLocation.getAltitude()));
                                climb += newLocation.getAltitude() - oldLocation.getAltitude();
                                Log.i("Total climb ", String.valueOf(climb));
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    points.add(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
                                    polyLine.setPoints(points);
                                    myMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(newLocation.getLatitude(), newLocation.getLongitude())));
                                    avgSpeedView.setText("Avg. avgSpeed: " + String.format("%.1f", avgSpeed) + " km/h.");
                                    curSpeedView.setText("Cur.speed: " + String.format("%.1f", curSpeed) + " km/h.");
                                    distanceView.setText("Distance travelled: " + String.valueOf(distance) + " m.");
                                    climbView.setText("Elevation change: " + String.valueOf(climb) + " m.");
                                    coordView.setText("Latitude: " + String.format("%.5f", newLocation.getLatitude()) + "\nLongitude: " + String.format("%.5f", newLocation.getLongitude()));
                                }
                            });
                            oldLocation = newLocation;
                        }
                    }
                };
                timer = new Timer();
                timer.scheduleAtFixedRate(t, 0, GPS_TIME_INTERVAL);
            }
        });

        stop = findViewById(R.id.stopBtn);
        stop.setClickable(false);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer.cancel();
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                notificationManager.cancel(17);
                start.setClickable(true);
            }
        });

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final PopupMenu popup = new PopupMenu(MainActivity.this, fab, Gravity.CENTER);
                popup.getMenuInflater().inflate(R.menu.add_new_popupmenu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu1:
                                // menu 1 selected
                                return true;
                            case R.id.menu2:
                                // menu 2 selected
                                popup.dismiss();
                                return true;
                            case R.id.menu3:
                                // menu 3 selected
                                popup.dismiss();
                                return true;
                            case R.id.menu4:
                                // menu 4 selected
                                popup.dismiss();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        myMap.addMarker(new MarkerOptions()
//                                .position(new LatLng(gpsServices.loc.getLatitude(), gpsServices.loc.getLongitude()))
//                                .title("test")
//                                .snippet("test's snippet")
//                                .draggable(true)
//                                .icon(BitmapDescriptorFactory.defaultMarker()));
//                    }
//                });
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

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
