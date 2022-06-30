package com.example.bluehound;

import static com.example.bluehound.Utilities.REQUEST_IMAGE_CAPTURE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.bluehound.ViewModel.AddViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddFragment extends Fragment {

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 23;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    TextInputEditText placeTIET;
    TextInputEditText descriptionTIET;

    private boolean requestingLocationUpdates = false;

    private RequestQueue requestQueue;
    private final static String OSM_REQUEST_TAG = "OSM_REQUEST";

    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkConnected = false;
    private Snackbar snackbar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity != null) {
            snackbar = Snackbar.make(activity.findViewById(R.id.fragment_container_view),
                    "No Internet Available", Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        }
                    });

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    isNetworkConnected = true;
                    snackbar.dismiss();
                    if(requestingLocationUpdates) {
                        startLocationUpdates(activity);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    isNetworkConnected = false;
                    snackbar.show();
                }
            };

            requestQueue = Volley.newRequestQueue(activity);
            requestPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            if (result) {
                                startLocationUpdates(activity);
                                Log.d("LAB-ADDFRAGMENT", "PERMISSION GRANTED");
                            } else {
                                Log.d("LAB-ADDFRAGMENT", "PERMISSION NOT GRANTED");
                                showDialog(activity);
                            }
                        }
                    });
            initializeLocation(activity);

            Utilities.setUpToolbar((AppCompatActivity) activity, "Add Travel");

            view.findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    if (takePicture.resolveActivity(activity.getPackageManager()) != null) {
                        activity.startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
                    }
                }
            });

            ImageView imageView = view.findViewById(R.id.picture_displayed_imageview);
            AddViewModel addViewModel = new ViewModelProvider((ViewModelStoreOwner) activity)
                    .get(AddViewModel.class);

            addViewModel.getImageBitmap().observe(getViewLifecycleOwner(), new Observer<Bitmap>() {
                @Override
                public void onChanged(Bitmap bitmap) {
                    imageView.setImageBitmap(bitmap);
                }
            });

            placeTIET = view.findViewById(R.id.name_edittext);
            TextInputEditText dateTIET = view.findViewById(R.id.date_edittext);
            descriptionTIET = view.findViewById(R.id.currentlocation_edittext);
            view.findViewById(R.id.fab_add).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bitmap
                            bitmap = addViewModel.getImageBitmap().getValue();

                    String imageUriString;
                    try {
                        if (bitmap != null) {
                            imageUriString = String.valueOf(saveImage(bitmap, activity));
                        } else {
                            imageUriString = "ic_baseline_android_24";
                        }
                        if (placeTIET.getText() != null && descriptionTIET.getText() != null
                                && dateTIET.getText() != null) {

                            addViewModel.addCardItem(new CardItem(imageUriString,
                                    placeTIET.getText().toString(), descriptionTIET.getText().toString(),
                                    dateTIET.getText().toString()));

                            addViewModel.setImageBitmap(null);

                            ((AppCompatActivity) activity).getSupportFragmentManager().popBackStack();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });

            view.findViewById(R.id.gps_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestingLocationUpdates = true;
                    registerNetworkCallback(activity);
                    startLocationUpdates(activity);
                }
            });
        }
    }

    /**
     * Method that saves the image taken by the user in the phone gallery.
     * @param bitmap the image taken by the user
     * @param activity the activity for this fragment
     * @throws FileNotFoundException if the file for the image was not created
     */
    private Uri saveImage(Bitmap bitmap, Activity activity) throws FileNotFoundException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY)
                .format(new Date());
        String name = "JPEG_" + timestamp + ".jpg";

        ContentResolver contentResolver = activity.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);

        Log.d("AddFragment", String.valueOf(imageUri));

        OutputStream outputStream = contentResolver.openOutputStream(imageUri);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUri;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        checkPermissionsForWriteExternalStorage();

    }

    private void checkPermissionsForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }


    /**
     *
     * @param menu The options menu in which you place your items.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.findItem(R.id.app_bar_search).setVisible(false);
    }

    private void initializeLocation(Activity activity) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        locationRequest = LocationRequest.create();
        //Set the desired interval for active location updates
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //Update UI with the location data
                Location location = locationResult.getLastLocation();
                /*String text = location.getLatitude() + ", " + location.getLongitude();
                placeTIET.setText(text);*/

                if (isNetworkConnected) {
                    sendVolleyRequest(String.valueOf(location.getLatitude()),
                            String.valueOf(location.getLongitude()));

                    requestingLocationUpdates = false;
                    stopLocationUpdates();
                } else {
                    snackbar.show();
                }
            }
        };
    }

    private void startLocationUpdates(Activity activity) {
        final String PERMISSION_REQUESTED = Manifest.permission.ACCESS_FINE_LOCATION;
        //permission granted
        if (ActivityCompat.checkSelfPermission(activity, PERMISSION_REQUESTED)
                == PackageManager.PERMISSION_GRANTED) {
            checkStatusGPS(activity);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } else if (ActivityCompat
                .shouldShowRequestPermissionRationale(activity, PERMISSION_REQUESTED)) {
            //permission denied before
            showDialog(activity);
        } else {
            //ask for the permission
            requestPermissionLauncher.launch(PERMISSION_REQUESTED);
        }

    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void showDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setMessage("Permission denied, but needed for gps functionality.")
                .setCancelable(false)
                .setPositiveButton("OK", ((dialogInterface, i) ->
                        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))))
                .setNegativeButton("Cancel", ((dialogInterface, i) -> dialogInterface.cancel()))
                .create()
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates && getActivity() != null){
            startLocationUpdates(getActivity());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void checkStatusGPS(Activity activity) {
        final LocationManager locationManager =
                (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        //if gps is off, show the alert message
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            new AlertDialog.Builder(activity)
                    .setMessage("Your GPS is off, do you want to enable it?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", ((dialogInterface, i) ->
                            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))))
                    .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel())
                    .create()
                    .show();
        }
    }

    private void sendVolleyRequest(String latitude, String longitude) {
        String url = "https://nominatim.openstreetmap.org/reverse?lat=" + latitude +
                "&lon="+ longitude + "&format=jsonv2&limit=1";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            placeTIET.setText(response.get("name").toString());
                            descriptionTIET.setText(response.get("display_name").toString());

                            unregisterNetworkCallback();
                        } catch (JSONException e) {
                            placeTIET.setText("/");
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("LAB-ADDFRAGMENT", error.toString());
                    }
                });

        jsonObjectRequest.setTag(OSM_REQUEST_TAG);
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(OSM_REQUEST_TAG);
        }
        if (requestingLocationUpdates)
            unregisterNetworkCallback();
    }

    private void registerNetworkCallback(Activity activity){
        ConnectivityManager connectivityManager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                isNetworkConnected = (networkInfo != null && networkInfo.isConnected());
            }
        } else {
            isNetworkConnected = false;
        }
    }

    private void unregisterNetworkCallback(){
        if (getActivity() != null){
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }
            } else {
                snackbar.dismiss();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() != null && requestingLocationUpdates){
            registerNetworkCallback(getActivity());
        }
    }
}
