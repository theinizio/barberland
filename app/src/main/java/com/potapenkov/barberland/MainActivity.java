package com.potapenkov.barberland;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.graphics.BitmapFactory.decodeStream;

public class MainActivity extends Activity  implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
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
    private Uri picUri;
    private Uri mImageCaptureUri;
    private ImageView mImageView;
    private File sourceFile;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 3;
    AlertDialog.Builder builder;
    public ProgressBar progressBar;
    private String uploadedFilePath=null;
    private HttpResponse response;
    private String barberPassword;
    private String clientPin;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setTheme(R.style.light6);



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .setAccountName("users.account.name@gmail.com")
                .build();
        mGoogleApiClient.connect();
        tf=Typeface.createFromAsset(getAssets(),"teslic.ttf");
        progressBar=(ProgressBar) findViewById(R.id.progressBar);

        setContentView(R.layout.search);
        currentViewId=R.layout.start_page;
        final String [] items			= new String [] {"Из камеры", "Из галереи"};
        ArrayAdapter<String> adapter	= new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,items);
         builder		= new AlertDialog.Builder(this);

        builder.setTitle("Выберите изображение");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { //pick from camera
                if (item == 0) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                            "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

                    try {
                        intent.putExtra("return-data", true);

                        startActivityForResult(intent, PICK_FROM_CAMERA);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                } else { //pick from file
                    Intent intent = new Intent();

                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
                }
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.v("onConnected","onConnected method");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Log.v("onConnected","got location");
            defaultLatLng=new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            if(currentViewId==R.layout.activity_main)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));


        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.v("conn", "suspended");
    }

    public void searchGPS(View v){

        showMap(defaultLatLng);
    }

    public void newCrop(View v){
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        String imageFileName = timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        //Log.v("file", "1615"+imageFileName+"  "+storageDir);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
       // mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        return image;
    }

    private int backPressedCount=0;
    @Override
    public void onBackPressed(){

        switch (currentViewId){
            case R.layout.new_barber:
            case R.layout.get_client_name:
                setContentView(R.layout.start_page);
                currentViewId=R.layout.start_page;
                backPressedCount=0;
                break;
            case R.layout.enter_pin:
                newClient(findViewById(R.id.newClientButton));
                //setContentView(R.layout.get_client_name);
                currentViewId=R.layout.get_client_name;
                backPressedCount=0;
                break;
            case R.layout.thanks:
                setContentView(R.layout.activity_main);
                currentViewId=R.layout.activity_main;
                backPressedCount=0;
                break;
            case R.layout.activity_main:
            case R.layout.start_page:
                if(++backPressedCount==2){
                    finish();
                }else Toast.makeText(this, "Нажмите повторно для выхода", Toast.LENGTH_SHORT).show();
                break;
        }

   }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case PICK_FROM_CAMERA:
                doCrop();
                break;

            case PICK_FROM_FILE:
                mImageCaptureUri = data.getData();
                doCrop();
                break;

            case CROP_FROM_CAMERA:
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");
                    ImageButton b=(ImageButton) findViewById(R.id.barber_photo);
                    b.setImageBitmap(photo);
                    String path = Environment.getExternalStorageDirectory().toString();
                    OutputStream fOut = null;
                    File file = null; // the File to save to
                    try {
                        file = createImageFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        fOut = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    Bitmap pictureBitmap = photo; // obtaining the Bitmap
                    pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                    try {
                        fOut.flush();
                        fOut.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                     // do not forget to close the stream

                    try {
                        MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    sourceFile=file;
                    new UploadFileToServer().execute();
                }
                File f = new File(mImageCaptureUri.getPath());

                if (f.exists()) f.delete();

                break;

        }
    }

    private void doCrop() {
        final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, 0 );

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();

            return;
        } else {
            intent.setData(mImageCaptureUri);

            intent.putExtra("outputX", 384);
            intent.putExtra("outputY", 512);
            intent.putExtra("aspectX", 3);
            intent.putExtra("aspectY", 4);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            if (size == 1) {
                Intent i 		= new Intent(intent);
                ResolveInfo res	= list.get(0);

                i.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                startActivityForResult(i, CROP_FROM_CAMERA);
            } else {
                for (ResolveInfo res : list) {
                    final CropOption co = new CropOption();

                    co.title 	= getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
                    co.icon		= getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
                    co.appIntent= new Intent(intent);

                    co.appIntent.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                    cropOptions.add(co);
                }

                CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Crop App");
                builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int item ) {
                        startActivityForResult( cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
                    }
                });

                builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel( DialogInterface dialog ) {

                        if (mImageCaptureUri != null ) {
                            getContentResolver().delete(mImageCaptureUri, null, null );
                            mImageCaptureUri = null;
                        }
                    }
                } );

                AlertDialog alert = builder.create();

                alert.show();
            }
        }
    }



    public void newClient(View v){
        setContentView(R.layout.get_client_name);
        currentViewId=R.layout.get_client_name;
        //Log.v("font", "1214font="+tf);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        TextView clientName = (TextView) findViewById(R.id.clientName);
        clientName.setText(settings.getString("clientName",""));
        clientName.setTypeface(tf);
        //Log.v("font", "1214client="+clientName);
        TextView clientPhone = (TextView) findViewById(R.id.client_phone_number);
        clientPhone.setText(settings.getString("clientPhone", ""));
        clientPhone.setTypeface(tf);
        TextView plus38 = (TextView) findViewById(R.id.plus38);
        plus38.setTypeface(tf);

    }
    
    public void newBarber(View v){
        setContentView(R.layout.new_barber);
        currentViewId=R.layout.new_barber;

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings.contains("uploadedFilePath")&& settings.getString(uploadedFilePath, "").length()>10 ) {
            File imgFile = new File(settings.getString(uploadedFilePath, ""));
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ImageButton myImage = (ImageButton) findViewById(R.id.barber_photo);
                myImage.setImageBitmap(myBitmap);
            }
        }

        TextView shedule=(TextView) findViewById(R.id.sheduleText);
        shedule.setTypeface(tf);
        TextView mon=(TextView)findViewById(R.id.isMonday);        mon.setTypeface(tf);
        TextView tue=(TextView)findViewById(R.id.isTuesday);        tue.setTypeface(tf);
        TextView wed=(TextView)findViewById(R.id.isWednesday);        wed.setTypeface(tf);
        TextView thu=(TextView)findViewById(R.id.isThursday);        thu.setTypeface(tf);
        TextView fri=(TextView)findViewById(R.id.isFriday);        fri.setTypeface(tf);
        TextView sat=(TextView)findViewById(R.id.isSaturday);        sat.setTypeface(tf);
        TextView sun=(TextView)findViewById(R.id.isSunday);        sun.setTypeface(tf);

        mon=(TextView)findViewById(R.id.oddOpenTime);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.oddFinishTime);        tue.setTypeface(tf);
        mon=(TextView)findViewById(R.id.evenFinishTime);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.evenOpenTime);        tue.setTypeface(tf);

        mon=(TextView)findViewById(R.id.isEven);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.isOdd);        tue.setTypeface(tf);

        mon=(TextView)findViewById(R.id.mondayOpenTime);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.tuesdayOpenTime);        tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.wednesdayOpenTime);        wed.setTypeface(tf);
        thu=(TextView)findViewById(R.id.thursdayOpenTime);        thu.setTypeface(tf);
        fri=(TextView)findViewById(R.id.fridayOpenTime);        fri.setTypeface(tf);
        sat=(TextView)findViewById(R.id.saturdayOpenTime);        sat.setTypeface(tf);
        sun=(TextView)findViewById(R.id.sundayOpenTime);        sun.setTypeface(tf);
        
        mon=(TextView)findViewById(R.id.mondayFinishTime);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.tuesdayFinishTime);        tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.wednesdayFinishTime);        wed.setTypeface(tf);
        thu=(TextView)findViewById(R.id.thursdayFinishTime);        thu.setTypeface(tf);
        fri=(TextView)findViewById(R.id.fridayFinishTime);        fri.setTypeface(tf);
        sat=(TextView)findViewById(R.id.saturdayFinishTime);        sat.setTypeface(tf);
        sun=(TextView)findViewById(R.id.sundayFinishTime);        sun.setTypeface(tf);

        mon=(TextView)findViewById(R.id.barber_name);        mon.setTypeface(tf);
        mon.setText(settings.getString("barberName",""));
        tue=(TextView)findViewById(R.id.barber_second_name);        tue.setTypeface(tf);
        tue.setText(settings.getString("barberSecondName",""));
        tue=(TextView)findViewById(R.id.plus38_2);  tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.barber_phone_number);        wed.setTypeface(tf);
        wed.setText(settings.getString("barberPhone",""));
        wed=(TextView)findViewById(R.id.barber_email);        wed.setTypeface(tf);
        wed.setText(settings.getString("barberEmail", ""));
        wed=(TextView)findViewById(R.id.isViber);        wed.setTypeface(tf);
        thu=(TextView)findViewById(R.id.salon_name);        thu.setTypeface(tf);
        thu.setText(settings.getString("salonName",""));
        fri=(TextView)findViewById(R.id.salon_address);        fri.setTypeface(tf);
        fri.setText(settings.getString("salonAddr", ""));
        sat=(TextView)findViewById(R.id.salon_phone);        sat.setTypeface(tf);
        sat.setText(settings.getString("salonPhone",""));
        sun=(TextView)findViewById(R.id.salonText);        sun.setTypeface(tf);

        mon=(TextView)findViewById(R.id.qualification);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.isBarber);        tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.isStylist);        wed.setTypeface(tf);
        thu=(TextView)findViewById(R.id.isMaster);        thu.setTypeface(tf);
        fri=(TextView)findViewById(R.id.isWomanCut);        fri.setTypeface(tf);
        sat=(TextView)findViewById(R.id.isManCut);        sat.setTypeface(tf);
        sun=(TextView)findViewById(R.id.isEvening);        sun.setTypeface(tf);

        mon=(TextView)findViewById(R.id.isColor);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.isHighLight);        tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.specializationText);        wed.setTypeface(tf);
        mon=(TextView)findViewById(R.id.m1);        mon.setTypeface(tf);
        tue=(TextView)findViewById(R.id.m2);        tue.setTypeface(tf);
        wed=(TextView)findViewById(R.id.m3);        wed.setTypeface(tf);
        thu=(TextView)findViewById(R.id.m4);        thu.setTypeface(tf);
        fri=(TextView)findViewById(R.id.m5);        fri.setTypeface(tf);
        sat=(TextView)findViewById(R.id.m6);        sat.setTypeface(tf);
        sun=(TextView)findViewById(R.id.m7);        sun.setTypeface(tf);
        chIsMonday=            (CheckBox) findViewById(R.id.isMonday);
        chIsTuesday=           (CheckBox) findViewById(R.id.isTuesday);
        chIsWednesday=         (CheckBox) findViewById(R.id.isWednesday);
        chIsThursday=          (CheckBox) findViewById(R.id.isThursday);
        chIsFriday=            (CheckBox) findViewById(R.id.isFriday);
        chIsSaturday=          (CheckBox) findViewById(R.id.isSaturday);
        chIsSunday=            (CheckBox) findViewById(R.id.isSunday);
        chIsOdd=               (CheckBox) findViewById(R.id.isOdd);
        chIsEven=              (CheckBox) findViewById(R.id.isEven);


        tvMonOpenTime=          (TextView) findViewById(R.id.mondayOpenTime);
        tvMonFinishTime=        (TextView) findViewById(R.id.mondayFinishTime);
        tvTueOpenTime=          (TextView) findViewById(R.id.tuesdayOpenTime);
        tvTueFinishTime=        (TextView) findViewById(R.id.tuesdayFinishTime);
        tvWedOpenTime=          (TextView) findViewById(R.id.wednesdayOpenTime);
        tvWedFinishTime=        (TextView) findViewById(R.id.wednesdayFinishTime);
        tvThuOpenTime=          (TextView) findViewById(R.id.thursdayOpenTime);
        tvThuFinishTime=        (TextView) findViewById(R.id.thursdayFinishTime);
        tvFriOpenTime=          (TextView) findViewById(R.id.fridayOpenTime);
        tvFriFinishTime=        (TextView) findViewById(R.id.fridayOpenTime);
        tvSatOpenTime=          (TextView) findViewById(R.id.saturdayOpenTime);
        tvSatFinishTime=        (TextView) findViewById(R.id.saturdayFinishTime);
        tvSunOpenTime=          (TextView) findViewById(R.id.sundayOpenTime);
        tvSunFinishTime=        (TextView) findViewById(R.id.sundayFinishTime);
        tvOddOpenTime=          (TextView) findViewById(R.id.oddOpenTime);
        tvOddFinishTime=        (TextView) findViewById(R.id.oddFinishTime);
        tvEvenOpenTime=          (TextView) findViewById(R.id.evenOpenTime);
        tvEvenFinishTime=        (TextView) findViewById(R.id.evenFinishTime);

    }

    public String cleanString(String dirty) {

        return Html.fromHtml(dirty).toString();

    }




    URL url = null;

    CheckBox chIsMonday;
    CheckBox chIsTuesday;
    CheckBox chIsWednesday;
    CheckBox chIsThursday;
    CheckBox chIsFriday;
    CheckBox chIsSaturday;
    CheckBox chIsSunday;
    CheckBox chIsOdd;
    CheckBox chIsEven;
    TextView tvMonOpenTime,tvMonFinishTime,tvTueOpenTime,tvTueFinishTime,tvWedOpenTime,tvWedFinishTime;
    TextView tvThuOpenTime,tvThuFinishTime,tvFriOpenTime,tvFriFinishTime,tvSatOpenTime,tvSatFinishTime;
    TextView tvSunOpenTime,tvSunFinishTime,tvOddOpenTime,tvOddFinishTime,tvEvenOpenTime,tvEvenFinishTime;


    public void checkNewBarberData(View v){
        JSONObject jo = new JSONObject();


        TextView tvBarberName=          (TextView) findViewById(R.id.barber_name);
        TextView tvBarberSecondName=    (TextView) findViewById(R.id.barber_second_name);
        TextView tvBarberPhoneNumber =  (TextView) findViewById(R.id.barber_phone_number);
        TextView tvBarberEmail =        (TextView) findViewById(R.id.barber_email);
        TextView tvSalonName=           (TextView) findViewById(R.id.salon_name);
        TextView tvSalonAddr =          (TextView) findViewById(R.id.salon_address);
        TextView tvSalonPhone=          (TextView) findViewById(R.id.salon_phone);

        CheckBox chIsViber=             (CheckBox) findViewById(R.id.isViber);


        String barberName=      cleanString(tvBarberName.       getText().toString());
        String barberSecondName=cleanString(tvBarberSecondName. getText().toString());
        String barberPhone=     cleanString(tvBarberPhoneNumber.getText().toString());
        String barberEmail=     cleanString(tvBarberEmail.      getText().toString());
        String salonName=       cleanString(tvSalonName.        getText().toString());
        String salonAddr=       cleanString(tvSalonAddr.        getText().toString());
        String salonPhone=      cleanString(tvSalonPhone.       getText().toString());
        String tmp;
        int  monOpenTime;
        tmp = cleanString(tvMonOpenTime.  getText().toString());
        if(tmp.length()>0) monOpenTime =   Integer.parseInt(tmp); else monOpenTime=9;
        int  monFinishTime;
        tmp = cleanString(tvMonFinishTime.getText().toString());
        if(tmp.length()>0) monFinishTime = Integer.parseInt(tmp); else monFinishTime=18;
        int  tueOpenTime;
        tmp = cleanString(tvTueOpenTime.  getText().toString());
        if(tmp.length()>0) tueOpenTime =   Integer.parseInt(tmp); else tueOpenTime=9;
        int  tueFinishTime;
        tmp = cleanString(tvTueFinishTime.getText().toString());
        if(tmp.length()>0) tueFinishTime = Integer.parseInt(tmp); else tueFinishTime=18;
        int  wedOpenTime;
        tmp = cleanString(tvWedOpenTime.  getText().toString());
        if(tmp.length()>0) wedOpenTime =   Integer.parseInt(tmp); else wedOpenTime=9;
        int  wedFinishTime;
        tmp = cleanString(tvWedFinishTime.getText().toString());
        if(tmp.length()>0) wedFinishTime = Integer.parseInt(tmp); else wedFinishTime=18;
        int  thuOpenTime;
        tmp = cleanString(tvThuOpenTime.  getText().toString());
        if(tmp.length()>0) thuOpenTime =   Integer.parseInt(tmp); else thuOpenTime=9;
        int  thuFinishTime;
        tmp = cleanString(tvThuFinishTime.getText().toString());
        if(tmp.length()>0) thuFinishTime = Integer.parseInt(tmp); else thuFinishTime=18;
        int  friOpenTime;
        tmp = cleanString(tvFriOpenTime.  getText().toString());
        if(tmp.length()>0) friOpenTime =   Integer.parseInt(tmp); else friOpenTime=9;
        int  friFinishTime;
        tmp = cleanString(tvFriFinishTime.getText().toString());
        if(tmp.length()>0) friFinishTime = Integer.parseInt(tmp); else friFinishTime=18;
        int  satOpenTime;
        tmp = cleanString(tvSatOpenTime.  getText().toString());
        if(tmp.length()>0) satOpenTime =   Integer.parseInt(tmp); else satOpenTime=9;
        int  satFinishTime;
        tmp = cleanString(tvSatFinishTime.getText().toString());
        if(tmp.length()>0) satFinishTime = Integer.parseInt(tmp); else satFinishTime=18;
        int  sunOpenTime;
        tmp = cleanString(tvSunOpenTime.  getText().toString());
        if(tmp.length()>0) sunOpenTime =   Integer.parseInt(tmp); else sunOpenTime=9;
        int  sunFinishTime;
        tmp = cleanString(tvSunFinishTime.getText().toString());
        if(tmp.length()>0) sunFinishTime = Integer.parseInt(tmp); else sunFinishTime=18;
        int  oddOpenTime;
        tmp = cleanString(tvOddOpenTime.  getText().toString());
        if(tmp.length()>0) oddOpenTime =   Integer.parseInt(tmp); else oddOpenTime=9;
        int  oddFinishTime;
        tmp = cleanString(tvOddFinishTime.getText().toString());
        if(tmp.length()>0) oddFinishTime = Integer.parseInt(tmp); else oddFinishTime=18;
        int  evenOpenTime;
        tmp = cleanString(tvEvenOpenTime.  getText().toString());
        if(tmp.length()>0) evenOpenTime =   Integer.parseInt(tmp); else evenOpenTime=9;
        int  evenFinishTime;
        tmp = cleanString(tvEvenFinishTime.getText().toString());
        if(tmp.length()>0) evenFinishTime = Integer.parseInt(tmp); else evenFinishTime=18;

        CheckBox chIsBarber=             (CheckBox) findViewById(R.id.isBarber);
        CheckBox chIsStylist=            (CheckBox) findViewById(R.id.isStylist);
        CheckBox chIsMaster=           (CheckBox) findViewById(R.id.isMaster);
        CheckBox chIsManCut=         (CheckBox) findViewById(R.id.isManCut);
        CheckBox chIsWomanCut=          (CheckBox) findViewById(R.id.isWomanCut);
        CheckBox chIsEvening=            (CheckBox) findViewById(R.id.isEvening);
        CheckBox chIsColor=          (CheckBox) findViewById(R.id.isColor);
        CheckBox chIsHighlight=            (CheckBox) findViewById(R.id.isHighLight);
        String errorStr="";
        try {
            jo.put("dataType", "barber");
            if(barberName.length()      >0) jo.put("barberName",        barberName);else errorStr+="Введите Ваше имя\n";
            if(barberPhone.length()     >0) jo.put("barberPhone",       "+38"+barberPhone);else errorStr+="Введите Ваш номер телефона\n";
            if(barberSecondName.length()>0) jo.put("barberSecondName",  barberSecondName);
            if(barberEmail.length()     >0) jo.put("barberEmail",       barberEmail);else errorStr+="Введите Ваш e-mail\n";
            if(uploadedFilePath!=null)      jo.put("uploadedFilePath",  uploadedFilePath);
            if(salonName.length()       >0) jo.put("salonName",         salonName);
            if(salonAddr.length()       >0) jo.put("salonAddr",         salonAddr);else errorStr+="Введите Ваш адрес\n";
            if(salonPhone.length()      >0) jo.put("salonPhone",        salonPhone);
                                            jo.put("isViber",           chIsViber.    isChecked());
                                            jo.put("isMonday",          chIsMonday.   isChecked());
                                            jo.put("isWednesday",       chIsWednesday.isChecked());
                                            jo.put("isTuesday",         chIsTuesday.  isChecked());
                                            jo.put("isThursday",        chIsThursday. isChecked());
                                            jo.put("isFriday",          chIsFriday.   isChecked());
                                            jo.put("isSaturday",        chIsSaturday. isChecked());
                                            jo.put("isSunday",          chIsSunday.   isChecked());
                                            jo.put("isOdd",             chIsOdd.      isChecked());
                                            jo.put("isEven",            chIsEven.     isChecked());
            jo.put("monOpenTime",   monOpenTime);
            jo.put("monFinishTime", monFinishTime);
            jo.put("tueOpenTime",   tueOpenTime);
            jo.put("tueFinishTime", tueFinishTime);
            jo.put("wedOpenTime",   wedOpenTime);
            jo.put("wedFinishTime", wedFinishTime);
            jo.put("thuOpenTime",   thuOpenTime);
            jo.put("thuFinishTime", thuFinishTime);
            jo.put("friOpenTime",   friOpenTime);
            jo.put("friFinishTime", friFinishTime);
            jo.put("satOpenTime",   satOpenTime);
            jo.put("satFinishTime", satFinishTime);
            jo.put("sunOpenTime",   sunOpenTime);
            jo.put("sunFinishTime", sunFinishTime);
            jo.put("oddOpenTime",   oddOpenTime);
            jo.put("oddFinishTime", oddFinishTime);
            jo.put("evenOpenTime",  evenOpenTime);
            jo.put("evenFinishTime",evenFinishTime);
            if(chIsBarber.isChecked())
                jo.put("qualification",        "barber");
            else if(chIsStylist.isChecked())
                jo.put("qualification",        "stylist");
            else if(chIsMaster.isChecked())
                jo.put("qualification",        "master");

            jo.put("isManCut",        chIsManCut.   isChecked());
            jo.put("isWomanCut",      chIsWomanCut. isChecked());
            jo.put("isEvening",       chIsEvening.  isChecked());
            jo.put("isColor",         chIsColor.    isChecked());
            jo.put("isHighlight",     chIsHighlight.isChecked());
            
            if(errorStr.length()>0){
                Toast.makeText(this, errorStr, Toast.LENGTH_SHORT).show();
            }else {

                // lets add some headers (nested JSON object)
                JSONObject header = new JSONObject();
                header.put("devicemodel", android.os.Build.MODEL); // Device model
                header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
                header.put("language", Locale.getDefault().getISO3Language()); // Language
                jo.put("header", header);
                JSONParser mJSONParser = new JSONParser(jo);
                mJSONParser.execute();
            }

            } catch (JSONException e) {
                e.printStackTrace();
            }





        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if(uploadedFilePath!=null)      editor.putString("uploadedFilePath",  uploadedFilePath);
        editor.putBoolean("barber_saved", true);
        editor.putString("barberName",        barberName);
        editor.putString("barberPhone",       barberPhone);
        editor.putString("barberSecondName",  barberSecondName);
        editor.putString("barberEmail",       barberEmail);
        editor.putString("salonName",         salonName);
        editor.putString("salonAddr",         salonAddr);
        editor.putString("salonPhone",        salonPhone);
        editor.putBoolean("isViber", chIsViber.isChecked());
        editor.putBoolean("isMonday", chIsMonday.isChecked());
        editor.putBoolean("isWednesday", chIsWednesday.isChecked());
        editor.putBoolean("isTuesday", chIsTuesday.isChecked());
        editor.putBoolean("isThursday", chIsThursday.isChecked());
        editor.putBoolean("isFriday", chIsFriday.isChecked());
        editor.putBoolean("isSaturday", chIsSaturday.isChecked());
        editor.putBoolean("isSunday", chIsSunday.isChecked());
        editor.putBoolean("isOdd", chIsOdd.isChecked());
        editor.putBoolean("isEven", chIsEven.isChecked());
        editor.putInt("monOpenTime", monOpenTime); editor.putInt("monFinishTime", monFinishTime);
        editor.putInt("tueOpenTime", tueOpenTime); editor.putInt("tueFinishTime", tueFinishTime);
        editor.putInt("wedOpenTime", wedOpenTime); editor.putInt("wedFinishTime", wedFinishTime);
        editor.putInt("thuOpenTime", thuOpenTime); editor.putInt("thuFinishTime", thuFinishTime);
        editor.putInt("friOpenTime", friOpenTime); editor.putInt("friFinishTime", friFinishTime);
        editor.putInt("satOpenTime", satOpenTime); editor.putInt("satFinishTime", satFinishTime);
        editor.putInt("sunOpenTime", sunOpenTime); editor.putInt("sunFinishTime", sunFinishTime);

        editor.putBoolean("isBarber",       chIsBarber.isChecked());
        editor.putBoolean("isStylist",      chIsStylist.isChecked());
        editor.putBoolean("isMaster",       chIsMaster.isChecked());

        editor.putBoolean("isManCut", chIsManCut.isChecked());
        editor.putBoolean("isWomanCut", chIsWomanCut.isChecked());
        editor.putBoolean("isEvening", chIsEvening.isChecked());
        editor.putBoolean("isColor", chIsColor.isChecked());
        editor.putBoolean("isHighlight", chIsHighlight.isChecked());
        // Commit the edits!
        editor.commit();
        showPin();
    }

    public  Drawable LoadImageFromWebOperations(String urlString) {
        try {
            URL url=new URL(UPLOADED_IMAGE_PATH+urlString);
            InputStream is = (InputStream) url.getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            Log.v("zzzb", "zzzb "+url.toString()+"|"+is.toString());
            return d;
        } catch (Exception e) {
            Log.v("zzzb", "zzzb "+e.getMessage());
            return null;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class JSONParser extends AsyncTask<Void, Integer, String> {
          private String responseString;
        private JSONObject dataToSend;
        // constructor
        public JSONParser(JSONObject j) {
            dataToSend=j;
                    }
        @Override
        protected String doInBackground(Void... params) {


            try {
                url = new URL("http://barberland.in.ua/write.php");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = null;
            try {
                httpPost = new HttpPost(url.toURI());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            // Prepare JSON to send by setting the entity
            try {
                //Log.v("back","vss234  "+dataToSen);
                if(dataToSend!=null)
                    httpPost.setEntity(new StringEntity(dataToSend.toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // Set up the header types needed to properly transfer JSON
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept-Encoding", "application/json");
            //httpPost.setHeader("Accept-Language", "en-US");

            // Execute POST
            try {
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity r_entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            return responseString;
        }

        @Override
        protected void onPostExecute(String res) {
            try {

                if (res.length() > 3)
                    res = res.substring(3);
                super.onPostExecute(res);

                //Log.v("pin", "pinv\n" + res + "|" + cleanString(res));
                if (res.length() > 16)
                    if (dataToSend == null) {
                        try {
                            barbers = new JSONObject(res);
                            setUpMap(startCoord);
                            //thanks(null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }catch (NullPointerException e){
                            showToast("start_coord not set");
                        }
                    } else showAlert(res);
                else {
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    if (res.contains("pin:")) {
                        String pin = cleanString(res.split(":")[1]/*.substring(4)*/);
                        clientPin = pin;
                        editor.putString("clientPin", pin);
                        showAlert(pin);
                    }
                    if (res.contains("pwd:")) {
                        String pwd = cleanString(res.substring(4));
                        barberPassword = pwd;
                        editor.putString("barberPassword", pwd);
                    }
                    editor.commit();
                }
            }catch(NullPointerException e){
                Toast.makeText(getApplicationContext(), "Нет связи с интернетом", Toast.LENGTH_SHORT).show();
                if(currentViewId==R.layout.get_client_name)
                    onBackPressed();
        }

        }
    }
    private void showToast(String txt){
        Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();

    }

    public void checkShedule(View v){
        try {
            if (chIsEven.isChecked() || chIsOdd.isChecked()) {
                chIsMonday.setEnabled(false);
                tvMonOpenTime.setEnabled(false);
                tvMonFinishTime.setEnabled(false);
                chIsTuesday.setEnabled(false);
                tvTueOpenTime.setEnabled(false);
                tvTueFinishTime.setEnabled(false);
                chIsWednesday.setEnabled(false);
                tvWedOpenTime.setEnabled(false);
                tvWedFinishTime.setEnabled(false);
                chIsThursday.setEnabled(false);
                tvThuOpenTime.setEnabled(false);
                tvThuFinishTime.setEnabled(false);
                chIsFriday.setEnabled(false);
                tvFriOpenTime.setEnabled(false);
                tvFriFinishTime.setEnabled(false);
                chIsSaturday.setEnabled(false);
                tvSatOpenTime.setEnabled(false);
                tvSatFinishTime.setEnabled(false);
                chIsSunday.setEnabled(false);
                tvSunOpenTime.setEnabled(false);
                tvSunFinishTime.setEnabled(false);
            } else if (chIsEven.isChecked() == false && chIsOdd.isChecked() == false) {
                chIsMonday.setEnabled(true);
                tvMonOpenTime.setEnabled(true);
                tvMonFinishTime.setEnabled(true);
                chIsTuesday.setEnabled(true);
                tvTueOpenTime.setEnabled(true);
                tvTueFinishTime.setEnabled(true);
                chIsWednesday.setEnabled(true);
                tvWedOpenTime.setEnabled(true);
                tvWedFinishTime.setEnabled(true);
                chIsThursday.setEnabled(true);
                tvThuOpenTime.setEnabled(true);
                tvThuFinishTime.setEnabled(true);
                chIsFriday.setEnabled(true);
                tvFriOpenTime.setEnabled(true);
                tvFriFinishTime.setEnabled(true);
                chIsSaturday.setEnabled(true);
                tvSatOpenTime.setEnabled(true);
                tvSatFinishTime.setEnabled(true);
                chIsSunday.setEnabled(true);
                tvSunOpenTime.setEnabled(true);
                tvSunFinishTime.setEnabled(true);
            }
            if (chIsMonday.isChecked() ||
                    chIsTuesday.isChecked() ||
                    chIsWednesday.isChecked() ||
                    chIsThursday.isChecked() ||
                    chIsFriday.isChecked() ||
                    chIsSaturday.isChecked() ||
                    chIsSunday.isChecked()) {
                chIsOdd.setEnabled(false);
                tvOddOpenTime.setEnabled(false);
                tvOddFinishTime.setEnabled(false);
                chIsEven.setEnabled(false);
                tvEvenOpenTime.setEnabled(false);
                tvEvenFinishTime.setEnabled(false);
            } else if (chIsMonday.isChecked() == false &&
                    chIsTuesday.isChecked() == false &&
                    chIsWednesday.isChecked() == false &&
                    chIsThursday.isChecked() == false &&
                    chIsFriday.isChecked() == false &&
                    chIsSaturday.isChecked() == false &&
                    chIsSunday.isChecked() == false) {
                chIsOdd.setEnabled(true);
                tvOddOpenTime.setEnabled(true);
                tvOddFinishTime.setEnabled(true);
                chIsEven.setEnabled(true);
                tvEvenOpenTime.setEnabled(true);
                tvEvenFinishTime.setEnabled(true);
            }
            if (tvOddOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvOddOpenTime.getText().toString()) < 0)
                    tvOddOpenTime.setText("0");
                if (Integer.parseInt(tvOddOpenTime.getText().toString()) > 24)
                    tvOddOpenTime.setText("24");
            }
            if (tvEvenOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvEvenOpenTime.getText().toString()) < 0)
                    tvEvenOpenTime.setText("0");
                if (Integer.parseInt(tvEvenOpenTime.getText().toString()) > 24)
                    tvEvenOpenTime.setText("24");
            }
            if (tvMonOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvMonOpenTime.getText().toString()) < 0)
                    tvMonOpenTime.setText("0");
                if (Integer.parseInt(tvMonOpenTime.getText().toString()) > 24)
                    tvMonOpenTime.setText("24");
            }
            if (tvTueOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvTueOpenTime.getText().toString()) < 0)
                    tvTueOpenTime.setText("0");
                if (Integer.parseInt(tvTueOpenTime.getText().toString()) > 24)
                    tvTueOpenTime.setText("24");
            }
            if (tvWedOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvWedOpenTime.getText().toString()) < 0)
                    tvWedOpenTime.setText("0");
                if (Integer.parseInt(tvWedOpenTime.getText().toString()) > 24)
                    tvWedOpenTime.setText("24");
            }
            if (tvThuOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvThuOpenTime.getText().toString()) < 0)
                    tvThuOpenTime.setText("0");
                if (Integer.parseInt(tvThuOpenTime.getText().toString()) > 24)
                    tvThuOpenTime.setText("24");
            }
            if (tvFriOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvFriOpenTime.getText().toString()) < 0)
                    tvFriOpenTime.setText("0");
                if (Integer.parseInt(tvFriOpenTime.getText().toString()) > 24)
                    tvFriOpenTime.setText("24");
            }
            if (tvSatOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvSatOpenTime.getText().toString()) < 0)
                    tvSatOpenTime.setText("0");
                if (Integer.parseInt(tvSatOpenTime.getText().toString()) > 24)
                    tvSatOpenTime.setText("24");
            }
            if (tvSunOpenTime.getText().length() != 0) {
                if (Integer.parseInt(tvSunOpenTime.getText().toString()) < 0)
                    tvSunOpenTime.setText("0");
                if (Integer.parseInt(tvSunOpenTime.getText().toString()) > 24)
                    tvSunOpenTime.setText("24");
            }


            if (tvOddFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvOddFinishTime.getText().toString()) < 0)
                    tvOddFinishTime.setText("0");
                if (Integer.parseInt(tvOddFinishTime.getText().toString()) > 24)
                    tvOddFinishTime.setText("24");
            }
            if (tvEvenFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvEvenFinishTime.getText().toString()) < 0)
                    tvEvenFinishTime.setText("0");
                if (Integer.parseInt(tvEvenFinishTime.getText().toString()) > 24)
                    tvEvenFinishTime.setText("24");
            }
            if (tvMonFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvMonFinishTime.getText().toString()) < 0)
                    tvMonFinishTime.setText("0");
                if (Integer.parseInt(tvMonFinishTime.getText().toString()) > 24)
                    tvMonFinishTime.setText("24");
            }
            if (tvTueFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvTueFinishTime.getText().toString()) < 0)
                    tvTueFinishTime.setText("0");
                if (Integer.parseInt(tvTueFinishTime.getText().toString()) > 24)
                    tvTueFinishTime.setText("24");
            }
            if (tvWedFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvWedFinishTime.getText().toString()) < 0)
                    tvWedFinishTime.setText("0");
                if (Integer.parseInt(tvWedFinishTime.getText().toString()) > 24)
                    tvWedFinishTime.setText("24");
            }
            if (tvThuFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvThuFinishTime.getText().toString()) < 0)
                    tvThuFinishTime.setText("0");
                if (Integer.parseInt(tvThuFinishTime.getText().toString()) > 24)
                    tvThuFinishTime.setText("24");
            }
            if (tvFriFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvFriFinishTime.getText().toString()) < 0)
                    tvFriFinishTime.setText("0");
                if (Integer.parseInt(tvFriFinishTime.getText().toString()) > 24)
                    tvFriFinishTime.setText("24");
            }
            if (tvSatFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvSatFinishTime.getText().toString()) < 0)
                    tvSatFinishTime.setText("0");
                if (Integer.parseInt(tvSatFinishTime.getText().toString()) > 24)
                    tvSatFinishTime.setText("24");
            }
            if (tvSunFinishTime.getText().length() != 0) {
                if (Integer.parseInt(tvSunFinishTime.getText().toString()) < 0)
                    tvSunFinishTime.setText("0");
                if (Integer.parseInt(tvSunFinishTime.getText().toString()) > 24)
                    tvSunFinishTime.setText("24");
            }

        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }


    public void checkQualification(View v){
        CheckBox other1;
        CheckBox other2;
        switch (v.getId()){
            case R.id.isBarber:
                other1 =(CheckBox) findViewById(R.id.isMaster);
                other1.setChecked(false);
                other2 =(CheckBox) findViewById(R.id.isStylist);
                other2.setChecked(false);
                break;
            case R.id.isMaster:
                other1 =(CheckBox) findViewById(R.id.isBarber);
                other1.setChecked(false);
                other2 =(CheckBox) findViewById(R.id.isStylist);
                other2.setChecked(false);
                break;
            case R.id.isStylist:
                other1 =(CheckBox) findViewById(R.id.isMaster);
                other1.setChecked(false);
                other2 =(CheckBox) findViewById(R.id.isBarber);
                other2.setChecked(false);
                break;
        }
    }

    public void savePhoneAndName(View v){
        TextView clientName =(TextView)findViewById(R.id.clientName);
        TextView clientPhone=(TextView)findViewById(R.id.client_phone_number);
        String clientPhoneText="+38"+cleanString(clientPhone.getText().toString());
    	if(checkNameAndPhone(cleanString(clientName.getText().toString()), clientPhoneText)){
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("clientName",  cleanString(clientName. getText().toString()));
            editor.putString("clientPhone", cleanString(clientPhone.getText().toString()));
            editor.commit();
            JSONObject jo=new JSONObject();
            try {
                jo.put("dataType", "client");jo.put("clientName", cleanString(clientName. getText().toString()));
                jo.put("clientPhone", clientPhoneText);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONParser mJSONParser = new JSONParser(jo);
            mJSONParser.execute();
    		showPin();
        }
    }
    private void showPin(){
        setContentView(R.layout.enter_pin);
        currentViewId=R.layout.enter_pin;
        TextView pin = (TextView) findViewById(R.id.pin_code);
        pin.setTypeface(tf);
    }
    private boolean checkNameAndPhone(String name, String phone){
        String errStr="";
        if(name.  length()== 0)errStr+="Введиде Ваше Имя\n";
        if(phone. length()!=13)errStr+="Введите номер телефона в формате +380...\n";
        if(errStr.length()>  0){
            Toast.makeText(getApplicationContext(), errStr, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void checkPin(View v){
        TextView pin = (TextView) findViewById(R.id.pin_code);
        if(clientPin!=null) {
            //Log.v("pinlen", "pinlen " + clientPin+" "+clientPin.length());
            if (clientPin.length() != 4) {
                Toast.makeText(getApplicationContext(), "Неправильный ответ от сервера", Toast.LENGTH_SHORT).show();
            }
            String pinText = cleanString(pin.getText().toString());
           // Log.v("pinlen", "pinlen " + clientPin+" "+clientPin.length()+"|"+pinText+"|"+(pinText.length() != 4)+"|"+(pinText != clientPin));
            if (pinText.length() != 4 || Integer.parseInt(pinText) != Integer.parseInt(clientPin)) {
                Toast.makeText(getApplicationContext(), "Неправильный пин-код", Toast.LENGTH_SHORT).show();
            } else {
                View vf=this.getCurrentFocus();
                if(vf!=null) {
                    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(vf.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                setContentView(R.layout.thanks);
                currentViewId = R.layout.thanks;
                showMap(defaultLatLng);
            }
        }
    }

    public void showMap(LatLng coord){
        setContentView(R.layout.activity_main);
        currentViewId=R.layout.activity_main;
        startCoord=coord;
        JSONParser getBarbers = new JSONParser(null);
        getBarbers.execute();

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




    private void setUpMap(LatLng coord)
    {
        mMarkersHashMap = new HashMap<Marker, MyMarker>();
        try {
            JSONObject arr;
            int qualification;
            for (int i=1;i<=barbers.length();i++){
                arr=barbers.getJSONObject(""+i);
                JSONArray specsJ=arr.getJSONArray("specializations");
                String[] specs=new String[specsJ.length()];
                for(int j=0;j<specsJ.length();j++){
                    specs[j]=(String)specsJ.get(j);
                }

                String qualifString=cleanString(arr.getString("qualification"));

                Log.v("json_barbers", "jbb "+qualifString+"|"+specs[0]);
                int qualif=MyMarker.mBARBER;
                if(qualifString.equalsIgnoreCase("парикмахер")) qualif=MyMarker.mBARBER;  else
                if(qualifString.equalsIgnoreCase("стилист"))qualif=MyMarker.mSTYLIST; else
                if(qualifString.equalsIgnoreCase("мастер")) qualif=MyMarker.mMASTER;

                final Bitmap[] bitmap=new Bitmap[1];
                final JSONObject finalArr = arr;
                final String photoPath=arr.getString("photo_path");
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
                            InputStream in = new URL(UPLOADED_IMAGE_PATH+cleanString(finalArr.getString("photo_path"))).openStream();

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

        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null)
        {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            // Check if we were successful in obtaining the map.
            mMap.setMyLocationEnabled(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coord, 15));
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

    public class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter
    {
        public MarkerInfoWindowAdapter()
        {
        }

        @Override
        public View getInfoWindow(Marker marker)
        {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker)
        {
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
        builder.setMessage("").setTitle("Квалификация")
                .setCancelable(false)
                .setPositiveButton("Понятно", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // do nothing
                    }
                });


        LinearLayout view = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.help, null);
        builder.setView(view);
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Uploading the file to server
     * */
    class UploadFileToServer extends AsyncTask<Void, Integer, String> {
        // File upload url (replace the ip with your server address)
        public static final String FILE_UPLOAD_URL = "http://barberland.in.ua/upload/fileUpload.php";

        // Directory name to store captured images and videos
        public static final String IMAGE_DIRECTORY_NAME = "Android File Upload";


        private String filePath = null;

        long totalSize = 0;


        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
            ProgressBar progressBar=(ProgressBar) findViewById(R.id.progressBar);
            if(progressBar!=null) {
                progressBar.setProgress(0);
            }
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
          //  progressBar.setVisibility(View.VISIBLE);

            // updating progress bar value
            ProgressBar progressBar=(ProgressBar) findViewById(R.id.progressBar);
            if(progressBar!=null) {
                progressBar.setProgress(progress[0]);
            }
            // updating percentage value
            //txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString = null;
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(FILE_UPLOAD_URL);
            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new AndroidMultiPartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });
                // Adding file data to http body
                entity.addPart("image", new FileBody(sourceFile));
                // Extra parameters if you want to pass to server
                entity.addPart("website", new StringBody("www.barberland.in.ua" ));
                entity.addPart("email",   new StringBody("barberland.in.ua@gmail.com"));
                totalSize = entity.getContentLength();
                httppost.setEntity(entity);
                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("TAG", "Response from server: " + result);

            // showing the server response in an alert dialog
            if(!result.contains("has been uploaded")) {
                showAlert(result);
                uploadedFilePath="";
            }else {
                uploadedFilePath=result.split("<br/>")[0];
               // showAlert(uploadedFilePath);
            }

        }

    }


    public void metroClickLitener(View v){
        Spinner mySpinner = (Spinner) findViewById(R.id.metro_spinner);
        String strChoose = mySpinner.getSelectedItem().toString();

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName("метро " + strChoose, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address = addresses.get(0);
        if(addresses.size() > 0) {
            double latitude = addresses.get(0).getLatitude();
            double longitude = addresses.get(0).getLongitude();
           showMap(new LatLng(latitude, longitude));

        }


    }


    /**
     * Method to show alert dialog
     * */
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





}
