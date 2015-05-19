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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.Plus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
    private SpinnerAdapter specializationsAdapter;
    private SpinnerAdapter qualificationsAdapter;
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

                    case MyMarker.mSALON:
                        markerOption.icon(BitmapDescriptorFactory.fromResource(R.drawable.salon));
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
            //Log.v("Pricessss","pricess "+res);
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

        Button b1 = (Button)findViewById(R.id.search_by_gps_button); b1.setTypeface(tf);
        Button b2 = (Button)findViewById(R.id.search_by_metro_button); b2.setTypeface(tf);
        Button b3 = (Button)findViewById(R.id.search_by_salon_name_button); b3.setTypeface(tf);
        Button b4 = (Button)findViewById(R.id.search_by_salon_price_button); b4.setTypeface(tf);
        Button b5 = (Button)findViewById(R.id.search_by_salon_service_type_button); b5.setTypeface(tf);
        Button b6 = (Button)findViewById(R.id.search_by_barber_name_button); b6.setTypeface(tf);
        Button b7 = (Button)findViewById(R.id.search_by_barber_specialization_button); b7.setTypeface(tf);
        Button b8 = (Button)findViewById(R.id.search_by_barber_qualification_button); b8.setTypeface(tf);

        TextView t1 = (TextView) findViewById(R.id.search_header_text); t1.setTypeface(tf);
        TextView t2 = (TextView) findViewById(R.id.search_by_location_text); t2.setTypeface(tf);
        TextView t3 = (TextView) findViewById(R.id.search_by_salon_text); t3.setTypeface(tf);
        TextView t4 = (TextView) findViewById(R.id.search_by_master_text); t4.setTypeface(tf);
        TextView t5 = (TextView) findViewById(R.id.search_barber_name); t5.setTypeface(tf);
        TextView t6 = (TextView) findViewById(R.id.search_by_time_text); t6.setTypeface(tf);


        Spinner metroSpinner   = (Spinner)findViewById(R.id.metro_spinner);

        SpinnerAdapter sa = new SpinnerAdapter(this);
        metroSpinner.setAdapter(sa);

        Spinner specializationsSpinner = (Spinner)findViewById(R.id.specialization_spinner);
        Spinner qualificationsSpinner  = (Spinner)findViewById(R.id.qualification_spinner);
        TextView spin = (TextView) findViewById(R.id.spinnertext);
        if(spin!=null)
            spin.setTypeface(tf);


        specializationsAdapter = new SpinnerAdapter(this);
        specializationsAdapter.colors.clear();

        specializationsSpinner.setAdapter(specializationsAdapter);
        qualificationsAdapter = new SpinnerAdapter(this);
        qualificationsAdapter.colors.clear();
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
                    specializationsAdapter.colors.add((String) specsJ.get(j));
                }
                specializationsAdapter.notifyDataSetChanged();
            }

            JSONArray qualsJ = jo.getJSONArray("qualifications");
            if (qualificationsAdapter != null) {
                for (int j = 1; j < qualsJ.length(); j++) {
                    qualificationsAdapter.colors.add((String) qualsJ.get(j));
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
                int qualif=qualificationsAdapter.colors.indexOf(qualifString)+1;
                /*
                Log.i("myQualification","myQualification "+qualificationsAdapter.getPosition(qualifString));

                if(qualifString.equalsIgnoreCase("парикмахер")) qualif=MyMarker.mBARBER;  else
                if(qualifString.equalsIgnoreCase("стилист"))qualif=MyMarker.mSTYLIST; else
                if(qualifString.equalsIgnoreCase("мастер")) qualif=MyMarker.mMASTER;
                */

                if(qualif==-1) qualif=MyMarker.mBARBER;

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

    private AlertDialog alert;
    public void showPriceSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_price_alert, null);
        builder.setView(view)
                .setCancelable(true);/*
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setPositiveButton("Искать", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //TODO Save settings to sharedprefs and send to server


                    }
                });*/
        alert = builder.create();
        alert.show();
          
        
        try {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            TextView t1 = (TextView) alert.findViewById(R.id.alert_man);t1.setTypeface(tf);
            TextView t2 = (TextView) alert.findViewById(R.id.alert_woman);t2.setTypeface(tf);
            TextView t3 = (TextView) alert.findViewById(R.id.alert_colorize);t3.setTypeface(tf);
            TextView t4 = (TextView) alert.findViewById(R.id.alert_hairdo);t4.setTypeface(tf);
            TextView t5 = (TextView) alert.findViewById(R.id.search_grn1);t5.setTypeface(tf);
            TextView t6 = (TextView) alert.findViewById(R.id.search_grn2);t6.setTypeface(tf);
            TextView t7 = (TextView) alert.findViewById(R.id.search_grn3);t7.setTypeface(tf);
            TextView t8 = (TextView) alert.findViewById(R.id.search_grn4);t8.setTypeface(tf);
            TextView t9 = (TextView) alert.findViewById(R.id.search_grn5);t9.setTypeface(tf);
            TextView t10 = (TextView) alert.findViewById(R.id.search_grn6);t10.setTypeface(tf);
            TextView t11 = (TextView) alert.findViewById(R.id.search_grn7);t11.setTypeface(tf);
            TextView t12 = (TextView) alert.findViewById(R.id.search_grn8);t12.setTypeface(tf);
            TextView t13 = (TextView) alert.findViewById(R.id.search_prices_man_cut_from);
            t13.setTypeface(tf);t13.setText(settings.getString("search_prices_man_cut_from",""));
            TextView t14 = (TextView) alert.findViewById(R.id.search_prices_woman_cut_from);
            t14.setTypeface(tf);t14.setText(settings.getString("search_prices_woman_cut_from", ""));
            TextView t15 = (TextView) alert.findViewById(R.id.search_prices_colorize_from);
            t15.setTypeface(tf);t15.setText(settings.getString("search_prices_colorize_from", ""));
            TextView t16 = (TextView) alert.findViewById(R.id.search_prices_hairdo_from);
            t16.setTypeface(tf);t16.setText(settings.getString("search_prices_hairdo_from", ""));
            TextView t17 = (TextView) alert.findViewById(R.id.search_prices_man_cut_to);
            t17.setTypeface(tf);t17.setText(settings.getString("search_prices_man_cut_to", ""));
            TextView t18 = (TextView) alert.findViewById(R.id.search_prices_woman_cut_to);
            t18.setTypeface(tf);t18.setText(settings.getString("search_prices_woman_cut_to", ""));
            TextView t19 = (TextView) alert.findViewById(R.id.search_prices_colorize_to);
            t19.setTypeface(tf);t19.setText(settings.getString("search_prices_colorize_to", ""));
            TextView t20 = (TextView) alert.findViewById(R.id.search_prices_hairdo_to);
            t20.setTypeface(tf);t20.setText(settings.getString("search_prices_hairdo_to",""));
        }catch (NullPointerException e){
            e.printStackTrace();
        }

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

    public void searchSalonPrices(View v){
        try {
            TextView t13 = (TextView) alert.findViewById(R.id.search_prices_man_cut_from);
            TextView t14 = (TextView) alert.findViewById(R.id.search_prices_woman_cut_from);
            TextView t15 = (TextView) alert.findViewById(R.id.search_prices_colorize_from);
            TextView t16 = (TextView) alert.findViewById(R.id.search_prices_hairdo_from);
            TextView t17 = (TextView) alert.findViewById(R.id.search_prices_man_cut_to);
            TextView t18 = (TextView) alert.findViewById(R.id.search_prices_woman_cut_to);
            TextView t19 = (TextView) alert.findViewById(R.id.search_prices_colorize_to);
            TextView t20 = (TextView) alert.findViewById(R.id.search_prices_hairdo_to);
            JSONObject jo = new JSONObject();
            jo.put("dataType", "searchByPrice");
            jo.put("search_prices_man_cut_from",  t13.getText());
            jo.put("search_prices_woman_cut_from",t14.getText());
            jo.put("search_prices_colorize_from", t15.getText());
            jo.put("search_prices_hairdo_from",   t16.getText());
            jo.put("search_prices_man_cut_to",    t17.getText());
            jo.put("search_prices_woman_cut_to",  t18.getText());
            jo.put("search_prices_colorize_to",   t19.getText());
            jo.put("search_prices_hairdo_to",     t20.getText());
            JSONObject header = new JSONObject();
            header.put("devicemodel", android.os.Build.MODEL); // Device model
            header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
            header.put("language", Locale.getDefault().getISO3Language()); // Language
            jo.put("header", header);
            JSONParser mJSONParser = new JSONParser(jo,this);
            mJSONParser.execute();
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("search_prices_man_cut_from", t13.getText().toString());
            editor.putString("search_prices_woman_cut_from", t14.getText().toString());
            editor.putString("search_prices_colorize_from", t15.getText().toString());
            editor.putString("search_prices_hairdo_from", t16.getText().toString());
            editor.putString("search_prices_man_cut_to", t17.getText().toString());
            editor.putString("search_prices_woman_cut_to", t18.getText().toString());
            editor.putString("search_prices_colorize_to", t19.getText().toString());
            editor.putString("search_prices_hairdo_to", t20.getText().toString());
            editor.commit();
            //FrameLayout a=(FrameLayout) v.getParent().getParent().getParent().getParent();
           // a.removeAllViewsInLayout();
            //Log.i("alert","alert"+a.getClass());
            if(alert!=null)alert.dismiss();
        }catch (NullPointerException e){
            e.printStackTrace();
        }catch (JSONException e){
            e.printStackTrace();
        }
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
