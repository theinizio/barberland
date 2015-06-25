package com.potapenkov.barberland;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kirill on 17.05.2015.
 */
public class NewClientActivity extends Activity {
    private LatLng defaultLatLng = new LatLng(50.520830, 30.606008);
    private LatLng startCoord;
    private int currentViewId = 0;
    private JSONObject barbers;
    public static final String PREFS_NAME = "barberlandSettings";
    private final String UPLOADED_IMAGE_PATH = "http://barberland.in.ua/upload/uploads/";
    private GoogleMap mMap;
    private ArrayList<MyMarker> mMyMarkersArray = new ArrayList<MyMarker>();
    private HashMap<Marker, MyMarker> mMarkersHashMap;
    private Typeface tf;
    private ArrayAdapter<String> specializationsAdapter;
    private ArrayAdapter<String> qualificationsAdapter;
    private Uri mImageCaptureUri;
    private Boolean getLocation = false;
    private File sourceFile;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 3;
    AlertDialog.Builder builder;
    public ProgressBar progressBar;
    private String uploadedFilePath = null;
    private URL url = null;
    private String barberPassword;
    private String clientPin;
    private GoogleApiClient mGoogleApiClient;
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tf = Typeface.createFromAsset(getAssets(), "teslic.ttf");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        Log.i("constructor", "pinOk=" + settings.getBoolean("pinOk", false));
        if (settings.getBoolean("pinOk", false)) {
            Log.v("gotomap", "gotomap");
            this.finish();
            Intent intent = new Intent(NewClientActivity.this, SearchActivity.class);
            //intent.putExtra("type", ViewPagerAdapter.SALON);
            startActivity(intent);
        } else {
            setContentView(R.layout.get_client_name);
            currentViewId = R.layout.get_client_name;

            TextView clientName = (TextView) findViewById(R.id.clientName);
            if (settings.contains("clientName"))
                clientName.setText(settings.getString("clientName", ""));
            clientName.setTypeface(tf);
            TextView clientPhone = (TextView) findViewById(R.id.client_phone_number);
            if (settings.contains("clientPhone"))
                clientPhone.setText(settings.getString("clientPhone", ""));
            clientPhone.setTypeface(tf);
            TextView plus38 = (TextView) findViewById(R.id.plus38);
            plus38.setTypeface(tf);
            Button skip = (Button) findViewById(R.id.skip_button);
            skip.setTypeface(tf);
        }
    }

    public void savePhoneAndName(View v) {
        TextView clientName = (TextView) findViewById(R.id.clientName);
        TextView clientPhone = (TextView) findViewById(R.id.client_phone_number);
        String clientPhoneText = "+38" + MainActivity.cleanString(clientPhone.getText().toString());
        if (checkNameAndPhone(MainActivity.cleanString(clientName.getText().toString()), clientPhoneText)) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("clientName", MainActivity.cleanString(clientName.getText().toString()));
            editor.putString("clientPhone", MainActivity.cleanString(clientPhone.getText().toString()));
            editor.commit();
            JSONObject jo = new JSONObject();
            try {
                jo.put("dataType", "newClient");
                jo.put("clientName", MainActivity.cleanString(clientName.getText().toString()));
                jo.put("clientPhone", clientPhoneText);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONParser mJSONParser = new JSONParser(jo, this);
            mJSONParser.execute();
            showPin();
        }
    }

    public void skipRegistration(View v) {
        //finish();
        Intent intent = new Intent(NewClientActivity.this, SearchActivity.class);
        //intent.putExtra("type", ViewPagerAdapter.SALON);
        startActivity(intent);
    }

    private void showPin() {
        setContentView(R.layout.enter_pin);
        currentViewId = R.layout.enter_pin;
        TextView pin = (TextView) findViewById(R.id.pin_code);
        pin.setTypeface(tf);
    }

    public void gotPin(String res) {
        if (res != null && res.length() == 4)
            clientPin = res;
    }

    private boolean checkNameAndPhone(String name, String phone) {
        String errStr = "";
        if (name.length() == 0) errStr += "Введиде Ваше Имя\n";
        if (phone.length() != 13) errStr += "Введите номер телефона в формате +380...\n";
        if (errStr.length() > 0) {
            Toast.makeText(getApplicationContext(), errStr, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void checkPin(View v) {
        TextView pin = (TextView) findViewById(R.id.pin_code);
        if (clientPin != null) {
            //Log.v("pinlen", "pinlen " + clientPin+" "+clientPin.length());
            if (clientPin.length() != 4) {
                Toast.makeText(getApplicationContext(), "Неправильный ответ от сервера", Toast.LENGTH_SHORT).show();
            }
            String pinText = MainActivity.cleanString(pin.getText().toString());
            // Log.v("pinlen", "pinlen " + clientPin+" "+clientPin.length()+"|"+pinText+"|"+(pinText.length() != 4)+"|"+(pinText != clientPin));
            if (pinText.length() != 4 || Integer.parseInt(pinText) != Integer.parseInt(clientPin)) {
                Toast.makeText(getApplicationContext(), "Неправильный пин-код", Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("pinOk", true);
                editor.putInt("lastPin", Integer.parseInt(clientPin));
                editor.commit();
                //Log.i("checkPin", "pinOk=" + settings.getBoolean("pinOk", false));
                //finish();
                Intent intent = new Intent(NewClientActivity.this, SearchActivity.class);
                //intent.putExtra("type", ViewPagerAdapter.SALON);
                startActivity(intent);
            }
        }
    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Ответ сервера")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showToast(String txt) {
        Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View vf = this.getCurrentFocus();
        if (vf != null) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(vf.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void onBackPressed() {
        if(currentViewId==R.layout.enter_pin)
            finish();
        Intent intent = new Intent(NewClientActivity.this, MainActivity.class);
        //intent.putExtra("type", ViewPagerAdapter.SALON);
        startActivity(intent);
    }
}