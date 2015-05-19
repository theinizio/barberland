package com.potapenkov.barberland;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class JSONParser extends AsyncTask<Void, Integer, String> {
    private LatLng defaultLatLng=new LatLng(50.520830, 30.606008);
    private LatLng startCoord;
    private int currentViewId=0;
    private JSONObject barbers;
    public static final String PREFS_NAME = "barberlandSettings";
    private final String UPLOADED_IMAGE_PATH="http://barberland.in.ua/upload/uploads/";
    private GoogleMap mMap;
    private ArrayList<MyMarker> mMyMarkersArray = new ArrayList<MyMarker>();
    private HashMap<Marker, MyMarker> mMarkersHashMap;
    private Typeface tf;
    private ArrayAdapter<String> specializationsAdapter;
    private ArrayAdapter<String> qualificationsAdapter;
    private Uri mImageCaptureUri;
    private Boolean getLocation=false;
    private File sourceFile;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 3;
    AlertDialog.Builder builder;
    public ProgressBar progressBar;
    private String uploadedFilePath=null;
    private URL url = null;
    private String barberPassword;
    private String clientPin;
    private GoogleApiClient mGoogleApiClient;



    private String responseString;
    private JSONObject dataToSend;
    private Object context;

    // constructor
    public JSONParser(JSONObject j, Object c) {
        this.dataToSend=j;
        this.context=c;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = null;
            url = new URL("http://barberland.in.ua/write.php");
            httpPost = new HttpPost(url.toURI());
            if(dataToSend!=null)
                httpPost.setEntity(new StringEntity(dataToSend.toString(), "UTF-8"));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept-Encoding", "application/json");
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity r_entity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200)
                responseString = EntityUtils.toString(r_entity);
             else
                responseString = "Error occurred! Http Status Code: " + statusCode;

        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (URISyntaxException e) {
            e.printStackTrace();
        }catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String res) {
        if(res!=null)
            if (res.length() > 3) res = res.substring(3);
        super.onPostExecute(res);
        //Log.i("res",res);
        try {

            String dataType=(String) dataToSend.get("dataType");

            if(dataType.equals("newClient")||dataType.equals("newBarber")||dataType.equals("newClient")){
                SharedPreferences settings = ((Context) context).getSharedPreferences(MainActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                if (res.contains("pin:")) {
                    String pin = MainActivity.cleanString(res.split(":")[1]);
                    if(context instanceof NewClientActivity) {
                        NewClientActivity nc = (NewClientActivity) context;
                        nc.gotPin(pin);
                    }
                    if(context instanceof NewBarberActivity) {
                        NewBarberActivity nc = (NewBarberActivity) context;
                        nc.gotPin(pin);
                    }
                    editor.putString("clientPin", pin);
                    editor.commit();
                    //if(settings.getBoolean("showPinOnScreen",false))
                        showAlert(pin);
                }
            }
            if (dataType.equals("specAndQualif")) {
                MapActivity mapActivity =(MapActivity)context;
                mapActivity.fillSpinners(res);
            }
            if(dataType.equals("showAll")){
                MapActivity mapActivity =(MapActivity)context;
                mapActivity.showAll(res);
            }
            if ( dataType.contains("search")) {
                MapActivity mapActivity =(MapActivity)context;
                mapActivity.showOnMapAfterSearch(res);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }catch(NullPointerException e) {
            e.printStackTrace();
            Toast.makeText((Context)context, "Нет связи с интернетом", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Log.i("res", res);
            e.printStackTrace();


        }
    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder((Context)context);
        builder.setMessage(message).setTitle("Ответ сервера")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
