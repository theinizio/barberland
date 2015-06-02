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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.ClusterManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
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
public class SearchActivity extends Activity
{

    private LatLng defaultLatLng=new LatLng(50.520830, 30.606008);
    private LatLng startCoord;
    private final String UPLOADED_IMAGE_PATH="http://barberland.in.ua/upload/uploads/";
    private GoogleMap mMap;
    private ArrayList<MyMarker> mMyMarkersArray = new ArrayList<MyMarker>();
    private HashMap<Marker, MyMarker> mMarkersHashMap;
    private Boolean getLocation=false;
    private GoogleApiClient mGoogleApiClient;
    private Typeface tf;
    private JSONObject barbers;
    private int currentViewId;
    private ClusterManager<MyMarker> mClusterManager;
    private ArrayList<String> specializations=new ArrayList<String>();
    private ArrayList<String> qualifications=new ArrayList<String>();
    private SpinnerAdapter metroSpinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        currentViewId=R.layout.search;
        tf=Typeface.createFromAsset(getAssets(),"teslic.ttf");
        customizeSpinner();
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.INVISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha(1);
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
                int qualif=qualifications.indexOf(qualifString)+1;
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



    public void showOnMapAfterSearch(String res){
        Intent intent = new Intent(SearchActivity.this, MapActivity.class);
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.INVISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha(1);
        intent.putExtra("res", res);
        startActivity(intent);
    }

    public void showAll(String res){
        Intent intent = new Intent(SearchActivity.this, MapActivity.class);
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.INVISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha(1);
        intent.putExtra("res", res);
        intent.putExtra("showAll",true);
        intent.putExtra("lat",startCoord.latitude);
        intent.putExtra("lng",startCoord.longitude);
        startActivity(intent);
    }

    private void customizeSpinner(){

        metroSpinnerAdapter = new SpinnerAdapter(this);

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
        TextView t6 = (TextView) findViewById(R.id.search_by_time_text); t6.setTypeface(tf);

        getSpecializationsAndQualifications();
    }

    public void fillSpinners(String res){
        try {
            JSONObject jo = new JSONObject(res);
            JSONArray specsJ = jo.getJSONArray("specializations");
            if (specializations != null) {
                for (int j = 1; j < specsJ.length(); j++) {
                    specializations.add((String) specsJ.get(j));
                }
            }
            JSONArray qualsJ = jo.getJSONArray("qualifications");
            if (qualifications != null) {
                for (int j = 1; j < qualsJ.length(); j++) {
                    qualifications.add((String) qualsJ.get(j));
                }
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
            ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
            p.setVisibility(View.VISIBLE);
            LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
            l.setAlpha((float) .5);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private AlertDialog alert;
    public void showPriceSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_price_alert, null);
        builder.setView(view)
                .setCancelable(true);
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
    public void showNameSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_name_alert, null);
        builder.setView(view)
                .setTitle("Введите имя/название")
                .setCancelable(true);
        alert = builder.create();
        alert.show();
        try {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            TextView t1 = (TextView) alert.findViewById(R.id.search_barber_name);t1.setTypeface(tf);

            t1.setText(settings.getString("search_barber_name", ""));

        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }
        public void showMetroSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_metro_alert, null);
        builder.setView(view)
                .setTitle("Выберите станцию")
                .setCancelable(true);
        alert = builder.create();
        alert.show();
        try {
            Spinner metroSpinner   = (Spinner)alert.findViewById(R.id.metro_spinner);
            metroSpinner.setAdapter(metroSpinnerAdapter);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    public void showSpinner(View v){
        try {
            Spinner s = (Spinner) findViewById(R.id.metro_spinner);
            s.performClick();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void showSpecsSearchDialog(View v){
        if(specializations.size()>0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LinearLayout view = (LinearLayout) getLayoutInflater()
                    .inflate(R.layout.search_by_specs_alert, null);
            builder.setView(view)
                    .setTitle("Выберите услугу")
                    .setCancelable(true);
            alert = builder.create();
            alert.show();
            try {
                LinearLayout l = (LinearLayout) alert.findViewById(R.id.search_specs_layout);
                //Log.i("colors", "size=" + specializations.size());
                for (int i = 0; i < specializations.size(); i++) {
                    CheckBox tmp = new CheckBox(alert.getContext());

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    tmp.setText(specializations.get(i));
                    tmp.setLayoutParams(lp);
                    tmp.setTypeface(tf);
                    tmp.setTextColor(getResources().getColor(R.color.text));
                    tmp.setTextSize(getResources().getDimension(R.dimen.text_size));
                    tmp.setId(1000 + i);
                    l.addView(tmp, i);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }else showToast("Данные не загружены");
    }

    public void showQualificatioSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_qual_alert, null);
        builder.setView(view)
                .setCancelable(true);
        alert = builder.create();
        alert.show();
        try {
            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
            RadioButton barb = (RadioButton) alert.findViewById(R.id.r_barber);
            RadioButton styl = (RadioButton) alert.findViewById(R.id.r_stylist);
            RadioButton mast = (RadioButton) alert.findViewById(R.id.r_master);
            barb.setTypeface(tf);
            styl.setTypeface(tf);
            mast.setTypeface(tf);
            barb.setChecked(settings.getBoolean("r_barber", true));
            styl.setChecked(settings.getBoolean("r_stylist", false));
            mast.setChecked(settings.getBoolean("r_master", false));
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    public void showTimeSearchDialog(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.search_by_time_alert, null);
        builder.setView(view)
                .setCancelable(true);
        alert = builder.create();
        alert.show();
        try {
            TimePicker timePicker = (TimePicker) alert.findViewById(R.id.timePicker);
            timePicker.setIs24HourView(true);
            timePicker.setCurrentHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
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
            if(location!=null) {
                lat = location.getLatitude();
                lon = location.getLongitude();
            }else{
                location=locationManager.getLastKnownLocation(providers.get(providers.size()-1));
                lat = location.getLatitude();
                lon = location.getLongitude();
            }
        } catch (NullPointerException e) {
             showToast("Не могу определить местоположение");
            lat=50.460209;
            lon=30.525824;
        }
        return new LatLng(lat, lon);
    }

    public void searchMetro(View v){
        Spinner mySpinner = (Spinner) alert.findViewById(R.id.metro_spinner);
        String strChoose = mySpinner.getSelectedItem().toString();
        LatLng decoded = metroSpinnerAdapter.coords.get(metroSpinnerAdapter.colors.indexOf(strChoose));
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<android.location.Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName("метро " + strChoose+ " Киев",2);
            //Log.i("adresses size=","adresses size="+addresses.size());
            if(addresses.size()>0){

                double latitude = addresses.get(0).getLatitude();
                double longitude = addresses.get(0).getLongitude();
                showMap(new LatLng(latitude, longitude));

                if(alert!=null)
                    alert.dismiss();
            }else showMap(decoded);//showToast("Не работает геокодер");
        } catch (Exception e) {
            //showToast("Не работает геокодер");
            showMap(decoded);
            e.printStackTrace();
        }
    }

    public void searchBarberName(View v){
        TextView bn = (TextView) alert.findViewById(R.id.search_barber_name);
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("search_barber_name", MainActivity.cleanString(bn.getText().toString()));
        editor.commit();
        if(MainActivity.cleanString(bn.getText().toString()).length()>0) {
            JSONObject jo = new JSONObject();
            try {
                jo.put("dataType", "searchBarberName");
                jo.put("barberNameToSearch", MainActivity.cleanString(bn.getText().toString()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new JSONParser(jo, this).execute();
            ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
            p.setVisibility(View.VISIBLE);
            LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
            l.setAlpha((float) 0.5);
            if(alert!=null)
                alert.dismiss();
        }else showToast("Введите имя");
    }

    public void searchBarberQualification(View v){
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        RadioButton barb = (RadioButton) alert.findViewById(R.id.r_barber);
        RadioButton styl = (RadioButton) alert.findViewById(R.id.r_stylist);
        RadioButton mast = (RadioButton) alert.findViewById(R.id.r_master);
        JSONObject jo = new JSONObject();
        int qualificationToSearch=1;
        if(barb.isChecked()) qualificationToSearch=(1);
        if(styl.isChecked()) qualificationToSearch=(2);
        if(mast.isChecked()) qualificationToSearch=(3);
        editor.putBoolean("r_barber", barb.isChecked());
        editor.putBoolean("r_stylist", styl.isChecked());
        editor.putBoolean("r_master", mast.isChecked());
        editor.commit();
        try {
            jo.put("dataType","searchBarberQualification");
            jo.put("qualificationToSearch",qualificationToSearch);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        new JSONParser(jo,this).execute();
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.VISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha((float) 0.5);
        if(alert!=null)
            alert.dismiss();
    }

    public void searchBarberSpecialization(View v){
        LinearLayout l = (LinearLayout)alert.findViewById(R.id.search_specs_layout);
        JSONObject jo = new JSONObject();
        String sToSearch="";
        try {
            jo.put("dataType","searchBarberSpecialization");
            for (int i = 0 ; i < (l.getChildCount()-1) ; i++ ) {
                CheckBox c = (CheckBox) l.getChildAt(i);
                if (c.isChecked()) {
                    if(sToSearch.length()>0)sToSearch += "|";
                    sToSearch +=""+(i+1);
                }
            }
            if(sToSearch.length()==0){
                showToast("Выберите хотя бы один пункт");
                return;
            }

            jo.put("specializationToSearch", sToSearch);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("parser", sToSearch);
        new JSONParser(jo,this).execute();
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.VISIBLE);
        LinearLayout l2 = (LinearLayout) findViewById(R.id.search_layout);
        l2.setAlpha((float) 0.5);
        if(alert!=null)
            alert.dismiss();
    }


    public void showMap(LatLng coord) {
        //hideKeyboard();

        startCoord = coord;
        JSONObject jo = new JSONObject();
        try {
            jo.put("dataType", "showAll");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONParser getBarbers = new JSONParser(jo, this);
        getBarbers.execute();
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.VISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha((float) 0.5);
        if(alert!=null)
            alert.dismiss();
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
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        p.setVisibility(View.VISIBLE);
        LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
        l.setAlpha((float) 0.5);
        if(alert!=null)
            alert.dismiss();
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
            ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
            p.setVisibility(View.VISIBLE);
            LinearLayout l = (LinearLayout) findViewById(R.id.search_layout);
            l.setAlpha((float) 0.5);

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

            if(alert!=null)alert.dismiss();
        }catch (NullPointerException e){
            e.printStackTrace();
        }catch (JSONException e){
            e.printStackTrace();
        }
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
