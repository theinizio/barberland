package com.potapenkov.barberland;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.Plus;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.graphics.BitmapFactory.decodeStream;

/**
 * Created by kirill on 17.05.2015.
 */
public class MapActivity extends Activity implements GoogleMap.OnMapLoadedCallback{

    private LatLng defaultLatLng=new LatLng(50.520830, 30.606008);
    private LatLng startCoord;
    private final String UPLOADED_IMAGE_PATH="http://barberland.in.ua/upload/uploads/";
    private GoogleMap mMap;
    private ArrayList<MyMarker> mMyMarkersArray = new ArrayList<MyMarker>();
    private HashMap<Marker, MyMarker> mMarkersHashMap;
    private Boolean getLocation=false;
    private GoogleApiClient mGoogleApiClient;
    private Typeface tf;
    private ArrayAdapter<String> specializationsAdapter;
    private ArrayAdapter<String> qualificationsAdapter;
    private JSONObject barbers;
    private int currentViewId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createGoogleClient();
        Intent i = getIntent();
        if(i.getBooleanExtra("showNewObject",false)){
            setContentView(R.layout.map);
            currentViewId=R.layout.map;
            startCoord=new LatLng(i.getDoubleExtra("newObjectLat",defaultLatLng.latitude),
                    i.getDoubleExtra("newObjectLng",defaultLatLng.longitude));
            setUpMap();
        }
        setContentView(R.layout.search);
        currentViewId=R.layout.search;
        tf=Typeface.createFromAsset(getAssets(),"teslic.ttf");
        customizeSpinner();

        //JSONParser getBarbers = new JSONParser(null);
       //getBarbers.execute();

    }

    private void createGoogleClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .setAccountName("users.account.name@gmail.com")
                .build();
        mGoogleApiClient.connect();
    }


    private void plotMarkers(ArrayList<MyMarker> markers)
    {
        if(markers.size() > 0)
        {
            for (MyMarker myMarker : markers)
            {
                // Create user marker with custom icon and other options
                MarkerOptions markerOption = new MarkerOptions().position(new LatLng(myMarker.getmLatitude(), myMarker.getmLongitude()));
                markerOption.flat(true);

                switch (myMarker.getmQualification()) {
                    default:
                    case MyMarker.mBARBER:
                        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.barber));
                        break;
                    case MyMarker.mSTYLIST:
                        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.stylist));
                        break;
                    case MyMarker.mMASTER:
                        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.master));
                        break;
                }

                Marker currentMarker = mMap.addMarker(markerOption);
                mMarkersHashMap.put(currentMarker, myMarker);
                mMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
            }
        }
    }

    public void showOnMapAfterSearch(String res){
        try {
            hideKeyboard();
            setContentView(R.layout.map);
            currentViewId=R.layout.map;
            JSONObject j = new JSONObject(res);
            JSONObject lastBarber = j.getJSONObject("" + j.length());
            startCoord = new LatLng(Double.parseDouble(lastBarber.getString("home_lat")),
                    Double.parseDouble(lastBarber.getString("home_long")));
            barbers = j;
            setUpMap();
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void showAll(String res){
        try {
            hideKeyboard();
            setContentView(R.layout.map);

            currentViewId=R.layout.map;
            JSONObject j = new JSONObject(res);
           // Log.v("res",res);
            barbers = j;
            setUpMap();
        }catch (JSONException e){
            Log.e("showAll","showAll JSONException");
            e.printStackTrace();
        }
    }

    private void customizeSpinner(){
        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker2);
        timePicker.setIs24HourView(true);

        timePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        //timePicker.setScaleX((float) 0.75);
        //timePicker.setScaleY((float) 0.75);

        Spinner metroSpinner   = (Spinner)findViewById(R.id.metro_spinner);
        Spinner specializationsSpinner = (Spinner)findViewById(R.id.specialization_spinner);
        Spinner qualificationsSpinner  = (Spinner)findViewById(R.id.qualification_spinner);
        TextView spin = (TextView) findViewById(R.id.spinnertext);
        //spin.setTypeface(tf);

        //spin=null;
        ArrayAdapter<CharSequence> metroAdapter =
                ArrayAdapter.createFromResource(this, R.array.metro_list, R.layout.simple_spinner_dropdown_item);
        metroAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        metroSpinner.setAdapter(metroAdapter);

        specializationsAdapter = new ArrayAdapter<String>(this,R.layout.simple_spinner_dropdown_item);
        specializationsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        specializationsSpinner.setAdapter(specializationsAdapter);
        qualificationsAdapter = new ArrayAdapter<String>(this,R.layout.simple_spinner_dropdown_item);
        qualificationsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        qualificationsSpinner.setAdapter(qualificationsAdapter);

        getSpecializationsAndQualifications();
        //spin.setTypeface(tf);
        spin= (TextView)findViewById(R.id.spinnerdropdown);
        //spin.setTypeface(tf);
    }

    public void fillSpinners(String res){
        try {
            JSONObject jo = new JSONObject(res);
            JSONArray specsJ = jo.getJSONArray("specializations");
            if (specializationsAdapter != null) {
                for (int j = 1; j < specsJ.length(); j++) {
                    specializationsAdapter.add((String) specsJ.get(j));
                }
                specializationsAdapter.notifyDataSetChanged();
            }

            JSONArray qualsJ = jo.getJSONArray("qualifications");
            if (qualificationsAdapter != null) {
                for (int j = 1; j < qualsJ.length(); j++) {
                    qualificationsAdapter.add((String) qualsJ.get(j));
                }
                qualificationsAdapter.notifyDataSetChanged();
            }

        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void getSpecializationsAndQualifications(){
        JSONObject jo=new JSONObject();
        try {
            jo.put("dataType", "specAndQualif");
            JSONParser qualifAndSpecs = new JSONParser(jo,this);
            qualifAndSpecs.execute();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getBarbersFromServer(){



        mMarkersHashMap = new HashMap<Marker, MyMarker>();
        try {
            JSONObject arr;

            for (int i=1;i<=barbers.length();i++){
                arr=barbers.getJSONObject(""+i);
                JSONArray specsJ=arr.getJSONArray("specializations");
                String[] specs=new String[specsJ.length()];
                for(int j=0;j<specsJ.length();j++){
                    specs[j]=(String)specsJ.get(j);
                }

                String qualifString=MainActivity.cleanString(arr.getString("qualification"));

                //Log.v("json_barbers", "jbb "+qualifString+"|"+specs[0]);
                int qualif=MyMarker.mBARBER;
                if(qualifString.equalsIgnoreCase("парикмахер")) qualif=MyMarker.mBARBER;  else
                if(qualifString.equalsIgnoreCase("стилист"))qualif=MyMarker.mSTYLIST; else
                if(qualifString.equalsIgnoreCase("мастер")) qualif=MyMarker.mMASTER;

                final Bitmap[] bitmap=new Bitmap[1];
                final JSONObject finalArr = arr;
                //final String photoPath=arr.getString("photo_path");
                final MyMarker tmpMarker=new MyMarker(arr.getString("name")+" "+arr.getString("second_name"),
                        arr.getString("photo_path"),
                        arr.getString("phone_number"),
                        qualif,
                        specs,
                        arr.getString("shedule"),
                        Double.parseDouble(arr.getString("home_lat")),
                        Double.parseDouble(arr.getString("home_long"))
                );

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            InputStream in = new URL(UPLOADED_IMAGE_PATH+MainActivity.cleanString(finalArr.getString("photo_path"))).openStream();
                            bitmap[0] = decodeStream(in);
                        } catch (Exception e) {e.printStackTrace();}
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        if (bitmap[0] != null)
                            tmpMarker.setmBitmap(bitmap[0]);
                        //Log.v("zzza", "zzza " +"|" + UPLOADED_IMAGE_PATH + cleanString(photoPath));
                    }
                }.execute();

                mMyMarkersArray.add(tmpMarker);
            }
            //Log.v("arr", "barbers "+arr);
        } catch (JSONException e) {
            e.printStackTrace();
            //Log.v("barbersFromJSON", "zzza Error");
        }
    }

    private void setUpMap()
    {
        //Log.v("setupMap", "seupMap was called");
        getBarbersFromServer();

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null)
        {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            mMap.setOnMapLoadedCallback(this);
            // Check if we were successful in obtaining the map.
            mMap.setMyLocationEnabled(true);


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startCoord, 15));
            //else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mMap.getMyLocation().getLatitude(),
            //       mMap.getMyLocation().getLongitude()),12));
            if (mMap != null)
            {
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
                {
                    @Override
                    public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker)
                    {
                        marker.showInfoWindow();
                        return true;
                    }
                });
            }
            else
                Toast.makeText(getApplicationContext(), "Unable to create Maps", Toast.LENGTH_SHORT).show();
        }
        plotMarkers(mMyMarkersArray);
    }


    @Override
    public void onMapLoaded() {
        if (mMap != null) {
            if(getLocation){
                Location l=mMap.getMyLocation();
                if(l!=null) {
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(l.getLatitude(),l.getLongitude()),14));
                    showToast(l.toString());
                    getLocation = false;
                }else showToast("Местоположение не установлено");
            }
        }
    }

/*
    @Override
    public void onBackPressed(){
        switch (currentViewId){
            case R.layout.map:
                setContentView(R.layout.search);
                currentViewId=R.layout.search;
                customizeSpinner();

                break;
            case R.layout.search:

                    finish();

                break;
        }
    }
*/
    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter
    {
        public MarkerInfoWindowAdapter(){}
        @Override
        public View getInfoWindow(Marker marker)
        {
            return null;
        }
        @Override
        public View getInfoContents(Marker marker){
            final View v  = getLayoutInflater().inflate(R.layout.infowindow_layout, null);
            final MyMarker myMarker = mMarkersHashMap.get(marker);
            final ImageView markerIcon = (ImageView) v.findViewById(R.id.marker_icon);
            TextView markerLabel =       (TextView)  v.findViewById(R.id.marker_label);
            TextView phone_number =      (TextView)  v.findViewById(R.id.phone_number);
            TextView sheduleText =       (TextView)  v.findViewById(R.id.sheduleText);
            TextView specializations =   (TextView)  v.findViewById(R.id.specializations);
            markerIcon.setImageBitmap(myMarker.getmBitmap());

            String specText="";
            for(int i=0;i<myMarker.getmSpecializations().length;i++){
                specText+=myMarker.getmSpecializations()[i]+"\n";
            }
            specializations.setText(specText);
            markerLabel.    setText(myMarker.getmLabel());
            phone_number.   setText(myMarker.getPhoneNumber());
            sheduleText.    setText(myMarker.getmShedule());
            return v;
        }
    }

    public void showHelp(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Квалификация")
                .setCancelable(false)
                .setPositiveButton("Понятно", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.help, null);
        builder.setView(view);
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void searchGPS(View v){
        LatLng l=_getLocation();
        Log.v("gps", "myLocation "+l.latitude+"|"+l.longitude);
        showMap(l);

    }

    private LatLng _getLocation() {
        // Get the location manager
        Double lat, lon;
        LocationManager locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        List<String> providers=locationManager.getAllProviders();


        try {
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Location location = locationManager.getLastKnownLocation(bestProvider);
            lat = location.getLatitude();
            lon = location.getLongitude();
            if(lat==null&&lon==null){
                location=locationManager.getLastKnownLocation(providers.get(providers.size()-1));
                lat = location.getLatitude();
                lon = location.getLongitude();
            }
        } catch (NullPointerException e) {
            lat = -1.0;
            lon = -1.0;
        }
        return new LatLng(lat, lon);
    }

    public void searchMetro(View v){
        Spinner mySpinner = (Spinner) findViewById(R.id.metro_spinner);
        String strChoose = mySpinner.getSelectedItem().toString();

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<android.location.Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName("метро " + strChoose+ "Киев", 1);
            android.location.Address address = addresses.get(0);
            if(addresses.size() > 0) {
                double latitude = addresses.get(0).getLatitude();
                double longitude = addresses.get(0).getLongitude();

                showMap(new LatLng(latitude, longitude));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            showToast("Неправильный адрес метро");
        }
    }

    public void searchBarberName(View v){
        TextView bn = (TextView) findViewById(R.id.search_barber_name);
        JSONObject jo = new JSONObject();
        try {
            jo.put("dataType","searchBarberName");
            jo.put("barberNameToSearch",MainActivity.cleanString(bn.getText().toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        new JSONParser(jo,this).execute();

    }

    public void searchBarberQualification(View v){
        Spinner qs = (Spinner) findViewById(R.id.qualification_spinner);
        //Log.v("qualification_selected", "qualification_selected "+qs.getSelectedItem());
        JSONObject jo = new JSONObject();
        try {
            jo.put("dataType","searchBarberQualification");
            jo.put("qualificationToSearch",MainActivity.cleanString((String) qs.getSelectedItem()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new JSONParser(jo,this).execute();
    }

    public void searchBarberSpecialization(View v){
        Spinner ss = (Spinner) findViewById(R.id.specialization_spinner);
        //Log.v("specialization_selected", "specialization_selected "+ss.getSelectedItem());
        JSONObject jo = new JSONObject();
        try {
            jo.put("dataType","searchBarberSpecialization");
            jo.put("specializationToSearch",MainActivity.cleanString((String) ss.getSelectedItem()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new JSONParser(jo,this).execute();
    }


    public void showMap(LatLng coord) {
        hideKeyboard();

        startCoord = coord;
        JSONObject jo = new JSONObject();
        try {
            jo.put("dataType", "showAll");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONParser getBarbers = new JSONParser(jo, this);
        getBarbers.execute();
    }

    public void searchSalonName(View v){

    }

    public void searchSalonPrices(View v){

    }

    public void searchSalonServices(View v){

    }

    public void searchByTime(View v){

    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Ответ сервера")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showToast(String txt){
        Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard(){
        View vf=this.getCurrentFocus();
        if(vf!=null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(vf.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

}
