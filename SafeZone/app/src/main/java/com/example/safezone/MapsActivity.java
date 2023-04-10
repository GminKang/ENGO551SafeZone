package com.example.safezone;

import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationRequestCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Build;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.safezone.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ThrowOnExtraProperties;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.protobuf.DescriptorProtos;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import android.Manifest;
import android.os.Looper;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,GoogleMap.OnMapLongClickListener, LocationListener, GeoQueryEventListener {

    private GoogleMap mMap;
    private static final String TAG = "MainActivity";
    private LocationManager locationManager;

    private Location lastLocation;

    private com.google.android.gms.location.LocationRequest locationRequest;

    private Marker currentLocationMarker;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private float GEOFENCE_RADIUS = 500;
    private int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;

    private boolean locationPermissionGranted = false;
    private ActivityMapsBinding binding;
    private LatLng geofenceLatLng;
    private static final String INTENT_ACTION = "com.example.safezone.transition";

    private GeofenceBroadcastReceiver receiver = new GeofenceBroadcastReceiver();
    private IntentFilter intentFilter = new IntentFilter(INTENT_ACTION);
    private BroadcastReceiver myBroadcastReceiver;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> safeZone;
    private LocationCallback locationCallback;


    private Boolean ENTERED = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);

                        mapFragment.getMapAsync(MapsActivity.this);
                        safeZone = new ArrayList<>();

                        settingGeofire();

                       // GeofenceBroadcastReceiver geofenceReceiver = new GeofenceBroadcastReceiver();
                       // registerReceiver(geofenceReceiver, GeofenceBroadcastReceiver.getIntentFilter());


                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You must enable location permission", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {

            public void onLocationResult(String key, GeoLocation location) {
                if (mMap != null) {
                    Log.d(TAG, "--------------------Setting LOcation");

                }
            }

        };
    }

    private void buildLocationRequest() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            com.google.android.gms.location.LocationRequest locationRequest = new LocationRequest()
                    .setInterval(5000)
                    .setPriority(PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(10f)
                    .setFastestInterval(3000);


        }


    }


    private void settingGeofire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(myLocationRef);
        Log.d(TAG, "------------------------------------------------------------geofire set");



    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (fusedLocationProviderClient != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        LocationUpdates();
        setUpMap();

        mMap.setOnMapLongClickListener(this);

        // Enable the user's current location if the location permission is granted


    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    private void setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_ACCESS_REQUEST_CODE);
            return;
        }
        mMap.setMyLocationEnabled(true);
        View locationButton = ((View) findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 250); // Add some margin to the bottom and right

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lastLocation = location;
                    LatLng currentLatLong = new LatLng(location.getLatitude(), location.getLongitude());
                    placeMarkerOnMap(currentLatLong);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f));
                }
            }
        });
    }

    private void placeMarkerOnMap(LatLng currentLatLong) {
        MarkerOptions markerOptions = new MarkerOptions().position(currentLatLong);
        markerOptions.title(currentLatLong.toString());
        mMap.addMarker(markerOptions);
    }

    private void LocationUpdates() {
        Log.d(TAG, "---------------LocationUpdates");
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, MapsActivity.this);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, MapsActivity.this);
    }




    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {

        addMarker(latLng);
        addCircle(latLng, GEOFENCE_RADIUS);
        geofenceLatLng = latLng;
        safeZone.add(latLng);
        Log.d(TAG, "safeZone"+safeZone);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude),0.5f); // 200m
        geoQuery.addGeoQueryEventListener(MapsActivity.this);

        FirebaseDatabase.getInstance()
                        .getReference("SafeZone")
                        .push()
                        .setValue(safeZone)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(MapsActivity.this, "Updated", Toast.LENGTH_SHORT).show();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

        Log.d(TAG, "geoQuery"+geoQuery);

        Log.d(TAG, "onSuccess: Geofence Added..."+latLng);

    }
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Log.d(TAG, "-------------ENTER");
        sendNotification("EDMDev", String.format("%s entered the safe zone",key));
    }

    @Override
    public void onKeyExited(String key) {
        Log.d(TAG, "-------------EXIT");
        sendNotification("EDMDev", String.format("%s leave the safe zone",key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("EDMDev", String.format("%s moving within the safe zone",key));

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(),Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "updating location");
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.d(TAG, "current location: "+ currentLatLng);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
        Log.d(TAG, "--------------------Setting Location");

        geoFire.setLocation("Your location", new GeoLocation(location.getLatitude(), location.getLongitude()),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (currentLocationMarker != null) currentLocationMarker.remove();
                        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title("Your location"));

                    }
                });



/*
        if (geofenceLatLng != null) {
            if (isInsideGeofence(location, geofenceLatLng)&& !ENTERED) {
                String message = "Entering Safe Zone";
                Intent intent = new Intent();
                intent.setAction(INTENT_ACTION);
                intent.putExtra("data",message);
                Log.d(TAG, "Entering log"+intent +intent.getStringExtra("data"));
                sendBroadcast(intent);
                ENTERED = true;

            }
            else if(!isInsideGeofence(location, geofenceLatLng) && ENTERED) {
                String message = "Leaving Safe Zone";
                Intent intent = new Intent();
                intent.setAction(INTENT_ACTION);
                intent.putExtra("data",message);
                Log.d(TAG, "Leaving log"+intent +intent.getStringExtra("data"));
                sendBroadcast(intent);
                ENTERED = false;

            }




        }

 */




    }


    private boolean isInsideGeofence(Location currentLocation, @NonNull LatLng geofenceLatLng) {
        float[] distance = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                geofenceLatLng.latitude, geofenceLatLng.longitude, distance);
        boolean r = distance[0] < GEOFENCE_RADIUS;
        Log.d(TAG, "inside? "+r);
        return distance[0] < GEOFENCE_RADIUS;
    }



    private void addMarker(LatLng latLng)
    {
        MarkerOptions markerOptions= new MarkerOptions().position(latLng);
        mMap.addMarker(markerOptions);
    }
    private void addCircle(LatLng latLng, float radius)
    {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255,255,0,0));
        circleOptions.fillColor(Color.argb(64,255,0,0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    public void searchLocation(View view) {
        EditText locationSearch = findViewById(R.id.Lsearch);
        String location = locationSearch.getText().toString().trim();
        List<Address> addressList = new ArrayList<>();
        if (location.isEmpty()) {
            Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
        } else {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addressList.size() > 0) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        };
    }



    private void sendNotification(String title, String content){
        Log.d(TAG, "sending notification");
        Toast.makeText(this,""+content,Toast.LENGTH_SHORT).show();
        String NOTIFICATION_CHANNEL_ID = "edmt_multiple_location";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Channel Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] {0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);


    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        return false;
    }
}
