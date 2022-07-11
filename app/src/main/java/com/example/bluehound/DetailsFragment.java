package com.example.bluehound;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import com.example.bluehound.ViewModel.ListViewModel;


import java.util.ArrayList;
import java.util.Random;

public class DetailsFragment extends Fragment {


    private TextView placeTextView;
    private TextView descriptionTextView;
    private TextView dateTextView;
    private TextView bltbonus;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private ImageView placeImageView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.details, container, false);
        Context ctx = v.getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        map = (MapView) v.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        return v;
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if(activity != null){
            Utilities.setUpToolbar((AppCompatActivity) activity, "Details");

            placeTextView = view.findViewById(R.id.place_name);
            descriptionTextView = view.findViewById(R.id.lastlocation);
            dateTextView = view.findViewById(R.id.travel_date);
            placeImageView = view.findViewById(R.id.place_image);
            bltbonus= view.findViewById(R.id.bltBonus);

            ListViewModel listViewModel =
                    new ViewModelProvider((ViewModelStoreOwner) activity).get(ListViewModel.class);
            listViewModel.getItemSelected().observe(getViewLifecycleOwner(), new Observer<CardItem>() {
                @Override
                public void onChanged(CardItem cardItem) {
                    placeTextView.setText(cardItem.getPlaceName());
                    descriptionTextView.setText(cardItem.getPlaceDescription());
                    dateTextView.setText(cardItem.getDate());
                    //foreach elem in Utilities.getConnectedDeviceInfo() add to bltbonus
                    if(cardItem.getStatus()=="Connected") {
                        int i = Utilities.getConnectedDeviceInfo().indexOf(cardItem.getPlaceName()) + 1;
                        int limit = i + 4;
                        for (; i < limit; i++) {
                            bltbonus.setText(bltbonus.getText() + "\n" + Utilities.getConnectedDeviceInfo().get(i));
                        }
                    }
                    else{
                        bltbonus.setText("");
                    }
                    String image_path = cardItem.getImageResource();
                    if (image_path.contains("ic_")){
                        Drawable drawable = ResourcesCompat.getDrawable(activity.getResources(),
                                R.drawable.ic_dog_running, activity.getTheme());
                        placeImageView.setImageDrawable(drawable);
                    } else {
                        Bitmap bitmap = Utilities.getImageBitmap(activity, Uri.parse(image_path));
                        if (bitmap != null){
                            placeImageView.setImageBitmap(bitmap);
                            placeImageView.setBackgroundColor(Color.WHITE);
                        }
                    }
                }
            });
            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);
            IMapController mapController = map.getController();
            mapController.setZoom(16.5);
            GeoPoint startPoint = listViewModel.getItemSelected().getValue().getGeoPoint();
            mapController.setCenter(startPoint);
            Marker startMarker = new Marker(map);
            startMarker.setPosition(startPoint);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(getResources().getDrawable(R.drawable.ic_dog_running));
            map.getOverlays().add(startMarker);
            view.findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, getText(R.string.device_name) + ": " +
                            placeTextView.getText().toString() +"\n" + getText(R.string.date) + ": " +
                            dateTextView.getText().toString() +"\n" + getText(R.string.description) + ": " +
                            descriptionTextView.getText().toString());
                    shareIntent.setType("text/plain");
                    Context context = view.getContext();
                    if (context != null && shareIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(Intent.createChooser(shareIntent, null));
                    }
                }
            });
            view.findViewById(R.id.track_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utilities.insertFragment((AppCompatActivity) activity, new TrackFragment(), TrackFragment.class.getSimpleName());
                }
            });
            view.findViewById(R.id.edit_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Utilities.setReplaceID(listViewModel.getItemSelected().getValue().getId());
                    Utilities.setReplaceFlag(true);
                    Utilities.insertFragment((AppCompatActivity) activity, new AddFragment(), AddFragment.class.getSimpleName());
                }
            });
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
        //update data

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this.getContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this.getActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }



}
