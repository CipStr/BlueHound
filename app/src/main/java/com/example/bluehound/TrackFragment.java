package com.example.bluehound;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.example.bluehound.ViewModel.ListViewModel;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;


public class TrackFragment extends Fragment {
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private RoadManager roadManager = null;
    private ListViewModel listViewModel;
    private GeoPoint oldLocation = null;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder builderProgress;
    private int PROGRESS_MAX = 100;
    private int PROGRESS_CURRENT = 0;
    private double totalDistance = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Configuration.getInstance().setUserAgentValue("MyOwnUserAgent/1.0");
        createNotificationChannel();
        notificationManager = NotificationManagerCompat.from(this.getActivity());
        builderProgress = new NotificationCompat.Builder(this.getContext(), "1")
                .setSmallIcon(R.drawable.ic_dog_running)
                .setContentTitle("Woof Woof")
                .setContentText("Taking your dog out for a walk?")
                .setSubText("Here's the progress you made!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.track, container, false);
        Context ctx = v.getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        roadManager = new OSRMRoadManager(this.getContext(),"MyOwnUserAgent/1.0");
        map = (MapView) v.findViewById(R.id.track_map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        return v;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity != null) {
            Utilities.setUpToolbar((AppCompatActivity) activity, "Track");
            listViewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(ListViewModel.class);
            setupMap();
        }
    }

    private void setupMap() {
        map.setMultiTouchControls(true);
        GeoPoint startPoint = Utilities.getLocation();
        IMapController mapController = map.getController();
        mapController.setZoom(16.5);
        //if center is already set, don't set it again
        if (oldLocation == null) {
            mapController.setCenter(startPoint);
        }
        Marker startMarker = new Marker(map);
        //manage null pointer exception
        if (startPoint == null) {
            startPoint = new GeoPoint(0, 0);
        }

        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Start point");
        startMarker.setIcon(getResources().getDrawable(R.drawable.ic_dog_running));
        ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
        waypoints.add(startPoint);
        GeoPoint endPoint = listViewModel.getItemSelected().getValue().getGeoPoint();
        //if function is called first time save total distance as distance between start and end point
        if (oldLocation == null) {
            float[] results = new float[1];
            Location.distanceBetween(startPoint.getLatitude(), startPoint.getLongitude(), endPoint.getLatitude(), endPoint.getLongitude(),results);
            totalDistance = results[0];
        }
        waypoints.add(endPoint);
        Road road = roadManager.getRoad(waypoints);
        Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
        Drawable nodeIcon = getResources().getDrawable(R.drawable.ic_pawprints);
        for (int i=0; i<road.mNodes.size(); i++){
            RoadNode node = road.mNodes.get(i);
            Marker nodeMarker = new Marker(map);
            nodeMarker.setPosition(node.mLocation);
            nodeMarker.setIcon(nodeIcon);
            nodeMarker.setTitle("Step "+i);
            nodeMarker.setSnippet(node.mInstructions);
            nodeMarker.setSubDescription(Road.getLengthDurationText(this.getContext(), node.mLength, node.mDuration));
            map.getOverlays().add(nodeMarker);
        }
        map.getOverlays().add(roadOverlay);
        map.getOverlays().add(startMarker);
        oldLocation = startPoint;
        //define current progress as distance from start to end
        float[] progressToMap= new float[1];
        Location.distanceBetween(startPoint.getLatitude(), startPoint.getLongitude(), endPoint.getLatitude(), endPoint.getLongitude(), progressToMap);
        //map progress as a percentage of total distance
        if(totalDistance <100){
            PROGRESS_CURRENT = 100;
        }
        else {
            PROGRESS_CURRENT = (int) ((PROGRESS_MAX * (totalDistance - progressToMap[0])) / totalDistance);
        }
        builderProgress.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
        notificationManager.notify(1, builderProgress.build());
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode, String[] permissions,
        int[] grantResults){
            ArrayList<String> permissionsToRequest = new ArrayList<>();
            for (int i = 0; i < grantResults.length; i++) {
                permissionsToRequest.add(permissions[i]);
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(
                        this.getActivity(),
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
    }

    private void requestPermissionsIfNecessary (String[]permissions){
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this.getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this.getActivity(),
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.scanBluetooth).setVisible(false);
        menu.findItem(R.id.enableBluetooth).setVisible(false);
        menu.findItem(R.id.visibleBluetooth).setVisible(false);
        menu.findItem(R.id.app_bar_search).setVisible(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    public void updateMap() {
        map.getOverlays().clear();
        setupMap();
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Notification Channel";
            String description = "Notification Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = this.getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
