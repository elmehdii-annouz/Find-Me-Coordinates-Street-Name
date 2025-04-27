package com.dev.localisation_app;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
/*import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;*/
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private TextView latitudeTextView, longitudeTextView, streetNameTextView;
    private WebView mapWebView;
    private Button refreshButton;

    private LocationCallback locationCallback; // Add a LocationCallback
    private LocationRequest locationRequest; // Add a LocationRequest

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
       MobileAds.initialize(this, initializationStatus -> {});


        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        latitudeTextView = findViewById(R.id.tv_latitude_value);
        longitudeTextView = findViewById(R.id.tv_longitude_value);
        streetNameTextView = findViewById(R.id.tv_street_name);
        mapWebView = findViewById(R.id.webview_map);
        refreshButton = findViewById(R.id.btn_refresh);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationRequest();

        // Set up click listeners to copy latitude and longitude to clipboard
        setUpCopyToClipboardListeners();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getLocationData();
        }

        // Set click listener for the refresh button
        refreshButton.setOnClickListener(v -> {
            getLocationData(); // Refresh the location data
        });
    }

    private void setupLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Update interval in milliseconds
        locationRequest.setFastestInterval(5000); // Fastest update interval
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // High accuracy
    }

    private void getLocationData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Initialize location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationUI(location);
                }
            }
        };

        // Start requesting location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

        // Get the last known location once
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLocationUI(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Convert to Decimal Degrees (DD) and DMS formats
        String latitudeDD = String.format(Locale.US, "%.6f", latitude);
        String longitudeDD = String.format(Locale.US, "%.6f", longitude);
        String latitudeDMS = convertToDMS(latitude, true);  // true for latitude
        String longitudeDMS = convertToDMS(longitude, false);  // false for longitude

        // Update TextViews with the formatted values
        latitudeTextView.setText(String.format("(DD): %s\n(DMS): %s", latitudeDD, latitudeDMS));
        longitudeTextView.setText(String.format("(DD): %s\n(DMS): %s", longitudeDD, longitudeDMS));

        // Fetch street name and display the map
        fetchStreetName(latitude, longitude);
        displayMap(latitude, longitude);
    }

    // Method to copy text to the clipboard
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    // Set up click listeners to copy latitude and longitude values
    private void setUpCopyToClipboardListeners() {
        latitudeTextView.setOnClickListener(v -> {
            String latitudeText = latitudeTextView.getText().toString();
            copyToClipboard("Latitude", latitudeText);
        });

        longitudeTextView.setOnClickListener(v -> {
            String longitudeText = longitudeTextView.getText().toString();
            copyToClipboard("Longitude", longitudeText);
        });

        streetNameTextView.setOnClickListener(v -> {
            String streetNameText = streetNameTextView.getText().toString();
            copyToClipboard("Street Name", streetNameText);
        });
    }

    private void fetchStreetName(double latitude, double longitude) {
        // Using Geocode.xyz API for reverse geocoding without requiring a token
        String url = "https://geocode.xyz/" + latitude + "," + longitude + "?geoit=json";

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Geocode.xyz API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(jsonData);

                        // Explicitly check for error or invalid responses
                        String errorMessage = jsonObject.optString("error", "");
                        String streetAddress = jsonObject.optString("staddress", "").trim();

                        // Check if the response contains typical throttling or error indicators
                        if (!errorMessage.isEmpty() ||
                                streetAddress.equalsIgnoreCase("Throttled!") ||
                                streetAddress.contains("Throttled") ||
                                streetAddress.equalsIgnoreCase("Address not found") ||
                                streetAddress.isEmpty()) {
                            Log.e(TAG, "Invalid address or error detected in API response");
                            return; // Skip updating the UI if the response is not valid
                        }

                        // Retrieve additional location info if available
                        String city = jsonObject.optString("city", "").trim();
                        String finalAddress = streetAddress + (city.isEmpty() ? "" : ", " + city);

                        // Update the UI with the valid address
                        runOnUiThread(() -> {
                            String streetName = "Street Name : " + finalAddress;

                            // Create a SpannableString
                            SpannableString spannableString = new SpannableString(streetName);

                            // Set "Street Name : " to be bold and size 22
                            int start = 0; // start index of "Street Name : "
                            int end = 14; // end index of "Street Name : " (length of this string)

                            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spannableString.setSpan(new RelativeSizeSpan(1.1f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Adjust size
                            spannableString.setSpan(new TextAppearanceSpan(null, 15, 0, null, null), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); // Set size to 22sp

                            // Set the SpannableString to the TextView
                            streetNameTextView.setText(spannableString);});

                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse Geocode.xyz response: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Geocode.xyz API call unsuccessful");
                }
            }
        });
    }



    // Updated displayMap method with OpenStreetMap
    private void displayMap(double latitude, double longitude) {
        String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                (longitude - 0.005) + "%2C" + (latitude - 0.005) + "%2C" + (longitude + 0.005) + "%2C" + (latitude + 0.005) +
                "&layer=mapnik&marker=" + latitude + "%2C" + longitude;

        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        mapWebView.loadUrl(mapUrl);
    }

    private String convertToDMS(double coordinate, boolean isLatitude) {
        // Determine if it's North/South or East/West
        String direction = "";
        if (isLatitude) {
            direction = coordinate >= 0 ? "N" : "S";
        } else {
            direction = coordinate >= 0 ? "E" : "W";
        }

        coordinate = Math.abs(coordinate); // Work with absolute values to simplify conversion

        int degrees = (int) coordinate;
        int minutes = (int) ((coordinate - degrees) * 60);
        int seconds = (int) (((coordinate - degrees) * 60 - minutes) * 60);

        // Format as DMS with the direction
        return String.format("%d°%d'%d\" %s", degrees, minutes, seconds, direction);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationData();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when the activity is not in the foreground
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Request location updates again when the activity is resumed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocationData();
        }
    }
}
