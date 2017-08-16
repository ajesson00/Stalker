package tutorial.maptutorial;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Toast;


import android.location.LocationListener;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    RequestQueue queue;
    LocationManager lm;
    Location location;
    String myPhoneNumber;
    MarkerOptions marker;
    LatLng otherLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //other setup stuff here
        setContentView(R.layout.activity_maps);
        queue = Volley.newRequestQueue(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        marker = new MarkerOptions();
        otherLocation = new LatLng(0, 0);
        lm  = (LocationManager)getSystemService(Context.LOCATION_SERVICE); //initialize lm-- fixed NullPointerException when requesting updates from lm

        ActivityCompat.requestPermissions(MapsActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);

        myPhoneNumber = getPhoneNumber();
    }


    private String getPhoneNumber () {
        TelephonyManager tMgr = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        //return tMgr.getLine1Number();
        return "9717777788";
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //updateLocationUI();

        if (mMap != null) {
            try {
                //enable my location
                mMap.setMyLocationEnabled(true);

            } catch (SecurityException e) {
                //had an error
            }

            mMap.getUiSettings().setMyLocationButtonEnabled(true);

        }

        //upload location to database
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                double lng = location.getLongitude();
                double lat = location.getLatitude();
                Calendar rightNow = Calendar.getInstance();
                String time = rightNow.get(Calendar.HOUR) + ":" + rightNow.get(Calendar.MINUTE) + ":" + rightNow.get(Calendar.SECOND);
                //Toast.makeText(getApplicationContext(), lat + " " + lng + " " + time, Toast.LENGTH_LONG).show();
                postNewComment(getApplicationContext(), myPhoneNumber, lat, lng, time);

                //check the other one
                contactServer(getApplicationContext()); //this will reset the marker to the given location
                //set a marker for other user's location
                if (otherLocation.latitude != 0 || otherLocation.longitude != 0) {
                    mMap.clear();
                    mMap.addMarker(marker);
                }

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        try {
            if (lm != null) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener); //NullPointerException from here?
            }
        } catch (SecurityException e) {
            //had an error
        }
        //uploadLocation();
    }




    private void contactServer(final Context context) {
        String url = "http://jaguar.cs.pdx.edu/~rfeng/test/?pn=5035047377";
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
                try {
                   JSONObject reader = new JSONObject(response);
                    JSONObject loc = reader.getJSONObject("location");
                    double lat, lng;
                    lat = loc.getDouble("latitude");
                    lng = loc.getDouble("longitude");
                    String pn, time;
                    pn = reader.getString("number");
                    time = reader.getString("time");
                    otherLocation = new LatLng(lat, lng);
                    //reset marker for other person's location
                    marker.position(otherLocation).title(pn + time);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            /*    //program throws this even when it successfully updates the server
                //really not sure why
            */
            }

        });
        queue.add(request);

    }

    private void postNewComment(final Context context, final String pn, final double lat, final double lng, final String time) {
        JSONObject params = new JSONObject();
        String url = "http://jaguar.cs.pdx.edu/~rfeng/test/";

        try {
            params.put("pn", pn);
            params.put("lat", lat);
            params.put("long", lng);
            params.put("time", time);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject o) {
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
            }
        });


        queue.add(jsonRequest); //adds new created request to queue with callback function for when it receives either a response or an error
    }
}
