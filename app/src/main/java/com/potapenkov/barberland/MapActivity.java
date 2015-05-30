package com.potapenkov.barberland;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.plus.Plus;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import java.util.List;
import java.util.Random;

import static android.graphics.BitmapFactory.decodeStream;

/**
 * Demonstrates heavy customisation of the look of rendered clusters.
 */
public class MapActivity extends FragmentActivity implements ClusterManager.OnClusterClickListener<MyMarker>, ClusterManager.OnClusterInfoWindowClickListener<MyMarker>, ClusterManager.OnClusterItemClickListener<MyMarker>, ClusterManager.OnClusterItemInfoWindowClickListener<MyMarker>, GoogleMap.OnInfoWindowClickListener{
    private ClusterManager<MyMarker> mClusterManager;
    private Random mRandom = new Random(1984);
    private LatLng defaultLatLng=new LatLng(50.520830, 30.606008);
    private LatLng startCoord;
    private final String UPLOADED_IMAGE_PATH="http://barberland.in.ua/upload/uploads/";
    private ArrayList<MyMarker> mMyMarkersArray = new ArrayList<MyMarker>();
    private JSONObject barbers;
    private GoogleApiClient mGoogleApiClient;



    /**
     * Draws profile photos inside markers (using IconGenerator).
     * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
     */
    private class MyMarkerRenderer extends DefaultClusterRenderer<MyMarker> {
        private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());
        private final ImageView mImageView;
        private final ImageView mClusterImageView;
        private final int mDimension;

        public MyMarkerRenderer() {
            super(getApplicationContext(), getMap(), mClusterManager);

            View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile, null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            mImageView = new ImageView(getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);

        }

        @Override
        protected void onBeforeClusterItemRendered(MyMarker person, MarkerOptions markerOptions) {
            // Draw a single person.
            // Set the info window to show their name.
            mImageView.setImageResource(person.getProfilePhoto());
            Bitmap icon = mIconGenerator.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .title(person.getmLabel() + "," + person.getPhoneNumber());

        }

        @Override
        protected void onBeforeClusterRendered(Cluster<MyMarker> cluster, MarkerOptions markerOptions) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = mDimension;
            int height = mDimension;

            for (MyMarker p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                Drawable drawable = getResources().getDrawable(p.getProfilePhoto());
                drawable.setBounds(0, 0, width, height);
                profilePhotos.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);

            mClusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 1;
        }
    }

    @Override
    public boolean onClusterClick(Cluster<MyMarker> cluster) {

        // Show a toast with some info when the cluster is clicked.
        String firstName = cluster.getItems().iterator().next().getmLabel();
        Toast.makeText(this, cluster.getSize() + " (including " + firstName + ")", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<MyMarker> cluster) {
        // Does nothing, but you could go to a list of the users.
    }

    @Override
    public boolean onClusterItemClick(MyMarker item) {

        // Does nothing, but you could go into the user's profile page, for example.
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(MyMarker item) {
        // Does nothing, but you could go into the user's profile page, for example.
    }



    private void addItems() {
        try {
            JSONObject jo = new JSONObject();
            jo.put("dataType", "showAll");
            new JSONParser(jo, this).execute();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private LatLng position() {
        return new LatLng(random(51.6723432, 51.38494009999999), random(0.148271, -0.3514683));
    }

    private double random(double min, double max) {
        return mRandom.nextDouble() * (max - min) + min;
    }


    public void showOnMapAfterSearch(String res){
        try {
            //Log.i("res", res);
            JSONObject j = new JSONObject(res);
            JSONObject lastBarber = j.getJSONObject("" + j.length());
            startCoord = new LatLng(Double.parseDouble(lastBarber.getString("home_lat")),
                    Double.parseDouble(lastBarber.getString("home_long")));
            barbers = j;
            getBarbersFromServer();
            getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(startCoord, 14));

        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void showAll(String res){
        try {
            //Log.i("res", res);
            JSONObject j = new JSONObject(res);
            barbers = j;
            getBarbersFromServer();
            //mClusterManager.addItems(mMyMarkersArray);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void getBarbersFromServer(){
        try {
            JSONObject arr;
            for (int i=1;i<=barbers.length();i++){
                arr=barbers.getJSONObject(""+i);
                JSONArray specsJ=arr.getJSONArray("specializations");
                String[] specs=new String[specsJ.length()];
                for(int j=0;j<specsJ.length();j++){
                    specs[j]=(String)specsJ.get(j);
                }
                String qualifString=cleanString(arr.getString("qualification"));
                int qualif=MyMarker.mBARBER;
                /*if(qualifString.equalsIgnoreCase("парикмахер")) qualif=MyMarker.mBARBER;  else
                if(qualifString.equalsIgnoreCase("стилист"))qualif=MyMarker.mSTYLIST; else
                if(qualifString.equalsIgnoreCase("мастер")) qualif=MyMarker.mMASTER; else*/
                if(qualifString.equalsIgnoreCase("салон")) qualif=MyMarker.mSALON;
                if(qualif==-1) qualif=MyMarker.mBARBER;
                final Bitmap[] bitmap=new Bitmap[1];
                final JSONObject finalArr = arr;
                final MyMarker tmpMarker=new MyMarker(arr.getString("name")+" "+arr.getString("second_name"),
                        arr.getString("photo_path"),
                        arr.getString("phone_number"),
                        qualif,
                        specs,
                        arr.getString("shedule_text"),
                        Double.parseDouble(arr.getString("home_lat")),
                        Double.parseDouble(arr.getString("home_long"))
                );
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            InputStream in = new URL(UPLOADED_IMAGE_PATH+finalArr.getString("photo_path")).openStream();
                            bitmap[0] = decodeStream(in);
                        } catch (Exception e) {e.printStackTrace();}
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        if (bitmap[0] != null)
                            tmpMarker.setmBitmap(bitmap[0]);
                    }
                }.execute();
                mMyMarkersArray.add(tmpMarker);
                mClusterManager.addItem(tmpMarker);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String cleanString(String dirty) {
        return Html.fromHtml(dirty).toString();
    }

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
            //Log.i("issled", "marker info " + marker.getTitle() + "|" + marker.getId());
            if(marker.getTitle()!=null) {
                MyMarker myMarker = searchMarker(marker.getTitle());
                final ImageView markerIcon = (ImageView) v.findViewById(R.id.marker_icon);
                TextView markerLabel = (TextView) v.findViewById(R.id.marker_label);
                TextView phone_number = (TextView) v.findViewById(R.id.phone_number);
                TextView sheduleText = (TextView) v.findViewById(R.id.sheduleText);
                TextView specializations = (TextView) v.findViewById(R.id.specializations);
                markerIcon.setImageBitmap(myMarker.getmBitmap());




                String specText = "";
                for (int i = 0; i < myMarker.getmSpecializations().length; i++) {
                    specText += myMarker.getmSpecializations()[i] + "\n";
                }
                specializations.setText(specText);
                markerLabel.setText(myMarker.getmLabel());
                phone_number.setText(myMarker.getPhoneNumber());
                sheduleText.setText(myMarker.getmShedule());

                return v;
            }else return null;
        }
    }

    private MyMarker searchMarker(String label){
        MyMarker mm=null;
        String[] sa=label.split(",");
        String name  = sa[0],
                phone = sa[1];

        for(int i=0;i<mMyMarkersArray.size();i++){
            mm=mMyMarkersArray.get(i);
            if(mm.getPhoneNumber().equals(phone)&&mm.getmLabel().equals(name))return mm;
        }
        return mm;
    }


    private GoogleMap mMap;

    protected int getLayoutId() {
        return R.layout.map;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
       // createGoogleClient();
        setUpMapIfNeeded();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }
    private void createGoogleClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .setAccountName("potapenkov@gmail.com")
                .build();
        mGoogleApiClient.connect();
    }
    private void setUpMapIfNeeded() {
        if (mMap != null) {
            return;
        }
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        // Check if we were successful in obtaining the map.
        mMap.setMyLocationEnabled(true);



        //mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if (mMap != null) {
            startMap();
        }
    }
    protected GoogleMap getMap() {
        setUpMapIfNeeded();
        return mMap;
    }

    protected void startMap() {
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.460209, 30.525824), 9.5f));

        mClusterManager = new ClusterManager<MyMarker>(this, getMap());
        mClusterManager.setRenderer(new MyMarkerRenderer());
        getMap().setOnCameraChangeListener(mClusterManager);
        getMap().setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(com.google.android.gms.maps.model.Marker marker) {
                getMap().moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                marker.showInfoWindow();
                return true;
            }
        });
        getMap().setOnInfoWindowClickListener(this);
        //getMap().setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        Intent i=getIntent();
        if(i.getBooleanExtra("showAll",false)){
            showAll(i.getStringExtra("res"));
            getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(

                    new LatLng(i.getDoubleExtra("lat",50.46),i.getDoubleExtra("lng",30.53)), 13));
        }else showOnMapAfterSearch(i.getStringExtra("res"));

        //addItems();
        mClusterManager.cluster();
        getMap().setInfoWindowAdapter(new MarkerInfoWindowAdapter());
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        String[] tmp=marker.getTitle().split(",");
        try {
            JSONObject jo = new JSONObject();
            jo.put("dataType", "showMeTheBarber");
            jo.put("phone", tmp[1]);

            new JSONParser(jo,this).execute();


        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    public void showDetailedBarber(String res){
        Intent intent = new Intent(MapActivity.this, NewBarberActivity.class);
        intent.putExtra("barber2show",res);
        intent.putExtra("readOnly",true);
        startActivity(intent);
    }

    public void showHelp(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Виды салонов:")
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

    public void sendCallbackRequest(View v){
        try {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            String number2call = settings.getString("clientPhone", "");

            if(number2call.length()==10) {
                showToast("Вам скоро перезвонят c ");
                JSONObject jo = new JSONObject();
                jo.put("dataType", "callback");
                jo.put("number2call", number2call);

            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
    private void showToast(String txt){
        Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(getApplicationContext(),"onDestroy()", Toast.LENGTH_SHORT).show();
    }
}
