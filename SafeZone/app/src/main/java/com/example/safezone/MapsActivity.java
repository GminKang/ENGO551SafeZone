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

import android.app.AlarmManager;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.safezone.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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
import com.google.android.gms.maps.GoogleMap;


import android.Manifest;
import android.os.Looper;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/*
public class MapsActivity: Google Maps Avtivity class

extends AppCompatActivity: built-in Android class that provides a base for activities that use the appcompat library features, such as a toolbar, action bar, or navigation drawer.

OnMapReadyCallback: An interface used to notify when the map is ready to be used.
GoogleMap.OnMarkerClickListener: An interface used to handle marker click events on the map.
GoogleMap.OnMapLongClickListener: An interface used to handle long click events on the map.
LocationListener: An interface used to handle location change events.
GeoQueryEventListener: An interface used to handle events related to GeoFire queries.
Overall, this class appears to be defining a MapsActivity that uses Google Maps, GeoFire, and location-based functionality in an Android application.
*/

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,GoogleMap.OnMapLongClickListener, LocationListener, GeoQueryEventListener {

    private GoogleMap mMap;
    private static final String TAG = "MainActivity";
    private LocationManager locationManager;

    private Location lastLocation;

    private com.google.android.gms.location.LocationRequest locationRequest;

    private Marker currentLocationMarker;
    private FusedLocationProviderClient fusedLocationProviderClient; //Getting the current user location
    private float GEOFENCE_RADIUS = 200; //A radius of the geofence
    private int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;

    private boolean locationPermissionGranted = false; //Check location permission
    private ActivityMapsBinding binding; //used to bind the layout for your activity with the corresponding views(needed to add the search bar!!).
    private LatLng geofenceLatLng; //center of the geofence
   // private static final String INTENT_ACTION = "com.example.safezone.transition";

   // private GeofenceBroadcastReceiver receiver = new GeofenceBroadcastReceiver();
//    private IntentFilter intentFilter = new IntentFilter(INTENT_ACTION);
 //   private BroadcastReceiver myBroadcastReceiver;
    private DatabaseReference myLocationRef;
    private GeoFire geoFire;
    private List<LatLng> safeZone;
    private LocationCallback locationCallback; // used to receive location updates from the device's location provider

    // Create a list of Marker and circle
    private HashMap<Marker, Circle> mMarkerCircleMap = new HashMap<>();
    private HashMap<Marker, GeoQuery> mMarkerKeyMap = new HashMap<>();
    private MaterialTimePicker picker;
    private Calendar calendar;

    private AlarmManager alarmManager;

    private Boolean INSIDE = false;
    private Boolean TRIGGER = false;

    private Boolean START = true;

    private Timer timer;



    /*
    onCreate is used to initialize the activity, such as setting the layout, initializing views.
    Called Dexter to request location permissions
    Once the permission is granted, it calls buildLocationCallback and buildLocationRequest
    buildLocationCallback - receives the device's location
    buildLocationRequest - sets locationRequest parameters:
        setInterval: location update interval
        setPriority: accuracy level of the location data
        setSmallestDisplacement - minimum distance the user moves before a new location update is sent
        setFastestInterval - fastest location update interval(useful when tracking)

     */
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

//settingGeofire :  initializes a Firebase Database reference to the "MyLocation" node, where the location data will be stored.
// Then, it creates a new GeoFire object with this reference.
    private void settingGeofire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("MyLocation");
        geoFire = new GeoFire(myLocationRef);
        Log.d(TAG, "------------------------------------------------------------geofire set");



    }
/*
    onMapReady :  adds the zoom control feature and get the current user location
    setUpMap: adds the current location icon
    LocationUpdates: updates the user's location
   setOnMapLongClickListener:  sets up a listener for when the user long presses on the map,

 */


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
       mMap.setOnMarkerClickListener(this);




    }

    //onStop: Removes location updates
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
                   // placeMarkerOnMap(currentLatLong);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f));
                }
            }
        });
    }

    //Adding a marker at the current location
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



/*
    onMapLongClick - Creating a geofence by long pressing the location and stores the data in Firebase

 */
    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {


        geofenceLatLng = latLng;
        safeZone.add(latLng);
        Log.d(TAG, "safeZone"+safeZone);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude),0.2f); // 200m
        geoQuery.addGeoQueryEventListener(MapsActivity.this);
        addMarkCircle(latLng, GEOFENCE_RADIUS, geoQuery);
        Button btn = (Button) findViewById(R.id.setTime);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
                setAlarm();
            }
        });



        FirebaseDatabase.getInstance()
                        .getReference("SafeZone")
                        .push()
                        .setValue(safeZone)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(MapsActivity.this, "Safe zone created", Toast.LENGTH_SHORT).show();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

        Log.d(TAG, "geoQuery"+geoQuery);

        Log.d(TAG, "onSuccess: Geofence Added..."+latLng);
        Toast.makeText(this, "Please set depart or arrival time", Toast.LENGTH_SHORT).show();

    }

    private void setAlarm() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

    }

    private void showTimePicker() {
        picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Time")
                .build();
        picker.show(getSupportFragmentManager(),"safezone");
        picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (picker.getHour()>12){
                    binding.setTime.setText(
                            String.format("%02d",picker.getHour()-12)+" : "+String.format("%02d",picker.getMinute())+"PM"
                    );
                }
                else{
                    binding.setTime.setText(
                            String.format("%02d",picker.getHour()-12)+" : "+String.format("%02d",picker.getMinute())+"AM"
                    );
                }
                calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY,picker.getHour());
                calendar.set(Calendar.MINUTE,picker.getMinute());
                calendar.set(Calendar.SECOND,0);
                calendar.set(Calendar.MILLISECOND,0);
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkTime();
                    }
                }, 0, 10000);

            }
        });


    }

    private void checkTime() {
        Calendar currentTime = Calendar.getInstance();
        if (currentTime.getTimeInMillis() >= calendar.getTimeInMillis()) {
            Log.d(TAG, "-------------CHECKING"+calendar.getTimeInMillis());
            TRIGGER = true;
        }
    }

    //Monitors the user's movement and send notification
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        Log.d(TAG, "-------------ENTER");
        sendNotification("EDMDev", String.format("%s ENTERING SAFE ZONE",key));
        if(!INSIDE){
            TRIGGER = false;
        }
    }

    @Override
    public void onKeyExited(String key) {
        Log.d(TAG, "-------------EXIT");
        sendNotification("EDMDev", String.format("%s ATTENTION! LEAVING THE SAFE ZONE",key));
        if(INSIDE){
            TRIGGER = false;
        }
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        //sendNotification("EDMDev", String.format("%s hanging out in the safe zone",key));

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
        Log.d(TAG, "INSIDE,TRIGGER"+INSIDE+TRIGGER);


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



        if (geofenceLatLng != null) {
            if(START){
                if (!isInsideGeofence(location, geofenceLatLng)){
                    INSIDE = false;

                }else{
                    INSIDE = true;

                }

                START = false;
            }
            if(!INSIDE) {

                if (!isInsideGeofence(location, geofenceLatLng) && TRIGGER) {

                    Toast.makeText(this, "Go to safe zone Right now! Calling the police in five minutes", Toast.LENGTH_SHORT).show();
                /*
                String message = "Entering Safe Zone";
                Intent intent = new Intent();
                intent.setAction(INTENT_ACTION);
                intent.putExtra("data",message);
                Log.d(TAG, "Entering log"+intent +intent.getStringExtra("data"));
                sendBroadcast(intent);
                ENTERED = true;

                 */

                }
            }
            if(INSIDE) {

                if (isInsideGeofence(location, geofenceLatLng) && TRIGGER) {

                    Toast.makeText(this, "You are staying here for too long. Calling the police in five minutes", Toast.LENGTH_SHORT).show();

                }
            }

        }

    }


    private boolean isInsideGeofence(Location currentLocation, @NonNull LatLng geofenceLatLng) {
        float[] distance = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                geofenceLatLng.latitude, geofenceLatLng.longitude, distance);
        boolean r = distance[0] < GEOFENCE_RADIUS;
        Log.d(TAG, "inside? "+r);
        return distance[0] < GEOFENCE_RADIUS;
    }


    private void addMarkCircle(LatLng latLng, float radius, GeoQuery geoQuery)
    {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255,255,0,0));
        circleOptions.fillColor(Color.argb(64,255,0,0));
        circleOptions.strokeWidth(4);
        Circle circle =  mMap.addCircle(circleOptions);
        MarkerOptions markerOptions= new MarkerOptions().position(latLng)
                .title("safe zone")
                .snippet("click to remove")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        Marker marker = mMap.addMarker(markerOptions);
        marker.setTag(false);
        mMarkerCircleMap.put(marker, circle);
        mMarkerKeyMap.put(marker, geoQuery);
    }

    // Adding a search bar
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
            public boolean onMarkerClick (@NonNull Marker marker){
            Circle circle = mMarkerCircleMap.get(marker);
            GeoQuery geoQuery = mMarkerKeyMap.get(marker);
            if (circle != null && geoQuery != null) {
                circle.remove();
                mMarkerCircleMap.remove(marker);
                mMarkerKeyMap.remove(marker);
                marker.remove();
                geofenceLatLng = null;
                geoQuery.removeAllListeners();
                Log.d(TAG, "--------------------Safe Zone discarded");
                Toast.makeText(this, "Safe Zone discarded.", Toast.LENGTH_SHORT).show();

                TRIGGER = false;
                Log.d(TAG, "TRIGGER" +TRIGGER);
                START = true;
                if (timer != null) {
                    calendar.clear();
                    timer.cancel();
                    timer = null;
                }


            }
            return true;
    }


}
