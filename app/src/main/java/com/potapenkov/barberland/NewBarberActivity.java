package com.potapenkov.barberland;


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
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.graphics.BitmapFactory.decodeStream;

/**
 * Created by Edwin on 15/02/2015.
 */
public class NewBarberActivity extends ActionBarActivity {

    public static final String PREFS_NAME = "barberlandSettings";
    private final String UPLOADED_IMAGE_PATH="http://barberland.in.ua/upload/uploads/";
    private final String defaultLogo = "http://barberland.in.ua/upload/uploads/default_logo.png";
    private Typeface tf;

    private Bitmap photo;
    private CheckBox chIsMonday;
    private CheckBox chIsTuesday;
    private CheckBox chIsWednesday;
    private CheckBox chIsThursday;
    private CheckBox chIsFriday;
    private CheckBox chIsSaturday;
    private CheckBox chIsSunday;
    private CheckBox chIsOdd;
    private CheckBox chIsEven;
    private TextView tvMonOpenTime,tvMonFinishTime,tvTueOpenTime,tvTueFinishTime,tvWedOpenTime,tvWedFinishTime;
    private TextView tvThuOpenTime,tvThuFinishTime,tvFriOpenTime,tvFriFinishTime,tvSatOpenTime,tvSatFinishTime;
    private TextView tvSunOpenTime,tvSunFinishTime,tvOddOpenTime,tvOddFinishTime,tvEvenOpenTime,tvEvenFinishTime;
    private boolean readOnly=false;

    private File sourceFile;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE   = 3;
    private static final int PICK_FROM_URL    = 4;
    private Uri mImageCaptureUri;
    AlertDialog.Builder builder;
    public ProgressBar progressBar;
    private String uploadedFilePath=null;
    ViewPager pager;
    ViewPagerAdapter adapter;
    SlidingTabLayout tabs;
    CharSequence Titles[]={"Общее","График работы","Виды работ","Цены"};
    int Numboftabs =4;
    private int barberOrSalon,
                currPos=-1,
                firstscroll=-1;
    private String clientPin;
    private AlertDialog alert;
    private String barber2show;
    private JSONObject jsonBarber=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_barber_2);

        Intent intent = getIntent();
        barberOrSalon =intent.getIntExtra("barberOrSalon", ViewPagerAdapter.BARBER);
        readOnly = intent.getBooleanExtra("readOnly", false);
        if(readOnly) {
            barber2show = intent.getStringExtra("barber2show");
            if (barber2show != null)
                extractJSON();
        }
        tf=Typeface.createFromAsset(getAssets(),"teslic.ttf");

        progressBar=(ProgressBar) findViewById(R.id.progressBar);


        setUpCrop(this);
        //Log.i("zzz","barberOrSalon="+barberOrSalon);
        // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
        adapter =  new ViewPagerAdapter(getSupportFragmentManager(),Titles,Numboftabs, barberOrSalon);

        // Assigning ViewPager View and setting the adapter
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(false); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width


        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });
        //tabs.setCustomTabView(R.layout.custom_tab, 0);
        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            // Called when the scroll state changes


            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                //Log.i("z123", "z123posScroll" + firstscroll + "|" + position);
                if(firstscroll==1) return;
                //Log.i("sel", "z123scrolled " + position);
                formSetUp(position);
                firstscroll=1;

                //tv.setBackground(R.drawable.ramka);

            }

            // This method will be invoked when a new page becomes selected

            @Override
            public void onPageSelected(int position)
            {
                //Log.i("z123","z123posSel"+currPos+"|"+position);
                if(currPos==position) return;
                formSetUp(position);
                //Log.i("sel", "z123selected "+position);
                switch (currPos){
                    case 0:
                        saveGeneral();
                        break;
                    case 1:
                        saveShedule();
                        break;
                    case 2:
                        saveQualification();
                        break;
                    case 3:
                        savePrices();
                        break;
                }
                currPos=position;

            }
        });
    }

    private void extractJSON() {
        try {

            JSONObject jo = new JSONObject(barber2show);
            jsonBarber=jo.getJSONObject("1");
            if(jsonBarber.has("isSalon"))
                if(jsonBarber.getString("isSalon").equals("true"))barberOrSalon=ViewPagerAdapter.SALON;
                else barberOrSalon=ViewPagerAdapter.BARBER;
        }catch (JSONException e){
            //barberOrSalon=ViewPagerAdapter.BARBER;
            showToast("не удалось загрузить данные");
            e.printStackTrace();
            Intent intent = new Intent(NewBarberActivity.this, MapActivity.class);
            //intent.putExtra("showNewObject", true);
            startActivity(intent);

        }
    }

    private AlertDialog dialog=null;
    public void newCrop(View v){
        dialog = builder.create();
        dialog.show();
    }

    private void setUpCrop(final Context c){
        final String [] items			= new String [] {"Из камеры", "Из галереи", "Из интернета"};
        final ArrayAdapter<String> adapter	= new ArrayAdapter<String> (this, android.R.layout.select_dialog_item,items);
        builder		= new AlertDialog.Builder(this);
        builder.setTitle("Выберите изображение");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { //pick from camera
                if (item == 0) { //pick from camera
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
                } else if (item == 1) { //pick from file
                    Intent intent = new Intent();

                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
                } else { //pick from URL
                    dialog.dismiss();
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(c);
                    LinearLayout view = (LinearLayout) getLayoutInflater()
                            .inflate(R.layout.search_by_name_alert, null);
                    builder2.setView(view)
                            .setTitle("Вставьте url")
                            .setCancelable(true);
                    alert = builder2.create();
                    alert.show();
                    try {
                        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                        TextView t1 = (TextView) alert.findViewById(R.id.search_barber_name);
                        t1.setTypeface(tf);
                        t1.setHint(defaultLogo);
                        t1.setText(settings.getString("imageURL", ""));

                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
    }

    public void searchBarberName(View v) {
        final TextView bn = (TextView) alert.findViewById(R.id.search_barber_name);
        String url = defaultLogo;
        if(bn.getText().length()>20){
            url=bn.getText().toString();
        }
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("imageURL", url);
        editor.commit();
        final Bitmap[] bitmap=new Bitmap[1];

        final ImageButton ib;
        if(barberOrSalon==ViewPagerAdapter.BARBER) ib = (ImageButton) findViewById(R.id.barber_photo);
        else ib = (ImageButton) findViewById(R.id.salon_logo);
        final String finalUrl = url;
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    InputStream in = new URL(finalUrl).openStream();
                    bitmap[0] = decodeStream(in);
                } catch (Exception e) {e.printStackTrace();}
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                if (bitmap[0] != null) {
                    Bitmap b = bitmap[0];
                    saveFile(b);
                    if(alert!=null)
                        alert.dismiss();

                    ib.setImageBitmap(resize(b));
                }
            }
        }.execute();
    }
    private Bitmap resize(Bitmap b){
        Matrix m = new Matrix();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        int reqWidth = width*4/10;
        int reqHeight = height*4/10;
        //Log.i("zzz", "sizes=("+width+","+height+")("+reqWidth+","+reqHeight+")");
        if(reqHeight<b.getHeight()||reqWidth<b.getWidth()) {
            m.setRectToRect(new RectF(0, 0, b.getWidth(), b.getHeight()), new RectF(0, 0, reqWidth, reqHeight), Matrix.ScaleToFit.CENTER);
            return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        }else
            return b;
    }
    private File saveFile(Bitmap bmp){
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        File file = null; // the File to save to
        try {
            file = createImageFile();
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush();
            fOut.close();
            savePath(file);
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return file;
    }
    private void savePath(File f){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        String tmp="";
        if(barberOrSalon==ViewPagerAdapter.SALON)tmp="absSalonPath";
        if(barberOrSalon==ViewPagerAdapter.BARBER)tmp="absPath";
        if(tmp.length()>0);
        editor.putString(tmp,f.getAbsolutePath());
        editor.commit();
    }


    private void doCrop() {
        final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);

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
                    photo = extras.getParcelable("data");
                    ImageButton b;
                    if(barberOrSalon==ViewPagerAdapter.BARBER)
                        b=(ImageButton) findViewById(R.id.barber_photo);
                    else
                        b=(ImageButton) findViewById(R.id.salon_logo);
                    b.setImageBitmap(resize(photo));
                    File file=saveFile(photo);
                    try {
                        MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    sourceFile=file;
                    new UploadFileToServer().execute();

                }
                File f = new File(mImageCaptureUri.getPath());

               // if (f.exists()) f.delete();

                break;

        }
    }



    private void formSetUp(int position) {
        Log.i("formSetup","formSetup "+position);
        try {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            switch (position) {
                case 0:
                    setUpGeneral(settings);
                    break;
                case 1:
                    setUpShedule(settings);
                    break;
                case 2:
                    setUpQualification(settings);
                    break;
                case 3:
                    setUpPrices(settings);
                    break;
            }

        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }




    private void setUpGeneral(SharedPreferences settings){
        try {
            if (barberOrSalon==ViewPagerAdapter.BARBER){//findViewById(R.id.barber_name) != null) {
                TextView barber_name         = (TextView) findViewById(R.id.barber_name);
                TextView barber_second_name  = (TextView) findViewById(R.id.barber_second_name);
                TextView plus38_2            = (TextView) findViewById(R.id.plus38_2);
                TextView barber_phone_number = (TextView) findViewById(R.id.barber_phone_number);
                TextView barber_email        = (TextView) findViewById(R.id.barber_email);
                TextView salon_name          = (TextView) findViewById(R.id.salon_name);
                TextView salon_address       = (TextView) findViewById(R.id.salon_address);
                TextView salon_phone         = (TextView) findViewById(R.id.salon_phone);
                TextView salonText           = (TextView) findViewById(R.id.salonText);
                CheckBox isViber             = (CheckBox) findViewById(R.id.isViber);
                final ImageButton myImage    = (ImageButton) findViewById(R.id.barber_photo);

                barber_name.        setTypeface(tf);
                barber_second_name. setTypeface(tf);
                plus38_2.           setTypeface(tf);
                barber_phone_number.setTypeface(tf);
                barber_email.       setTypeface(tf);
                isViber.            setTypeface(tf);
                salon_name.         setTypeface(tf);
                salon_address.      setTypeface(tf);
                salon_phone.        setTypeface(tf);
                salonText.          setTypeface(tf);
                if(readOnly&&jsonBarber!=null){
                    myImage.setOnClickListener(null);
                    salon_phone.        setKeyListener(null);
                    salon_address.      setKeyListener(null);
                    salon_name.         setKeyListener(null);
                    barber_email.       setKeyListener(null);
                    barber_phone_number.setKeyListener(null);
                    barber_second_name. setKeyListener(null);
                    barber_name.        setKeyListener(null);
                    isViber.setVisibility(View.INVISIBLE);
                    LinearLayout l = (LinearLayout) findViewById(R.id.new_barber_phone_layout);
                    l.removeView(isViber);
                    Button callback =new Button(l.getContext());
                    callback.setText("callback");
                    callback.setTextSize(getResources().getDimension(R.dimen.text_size));
                    callback.setTypeface(tf);
                    callback.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            callback(v);
                        }
                    });
                    l.addView(callback);
                    barber_name.        setText(jsonBarber.getString("name"));
                    barber_second_name. setText(jsonBarber.getString("second_name"));
                    barber_phone_number.setText(jsonBarber.getString("phone_number").substring(3));
                    barber_email.       setText(jsonBarber.getString("email"));
                    salon_name.         setText(jsonBarber.getString("salon_name"));
                    salon_address.      setText(jsonBarber.getString("salon_address"));
                    salon_phone.        setText(jsonBarber.getString("salon_phone"));

                    Log.i("zzz", "layout_before=" + l.getWidth());
                    new AsyncTask<Void, Void, Void>() {
                        public Bitmap[] bitmap=new Bitmap[1];

                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                URL url = new URL(UPLOADED_IMAGE_PATH+jsonBarber.getString("photo_path").substring(1));
                                //Log.i("zzz","Barber_path="+url.getPath());
                                InputStream in = url.openStream();
                                bitmap[0] = decodeStream(in);
                            } catch (Exception e) {e.printStackTrace();}
                            return null;
                        }
                        @Override
                        protected void onPostExecute(Void result) {
                            if (bitmap[0] != null) {
                                myImage.setImageBitmap(resize(bitmap[0]));
                                LinearLayout l=(LinearLayout)findViewById(R.id.new_barber_photo_layout);

                                Log.i("zzz", "image=" + bitmap[0].getWidth()+"|"+l.getWidth());
                            }
                        }
                    }.execute();

                }else {
                    barber_name.        setText(settings.getString("barberName", ""));
                    barber_second_name. setText(settings.getString("barberSecondName", ""));
                    barber_phone_number.setText(settings.getString("barberPhone", ""));
                    barber_email.       setText(settings.getString("barberEmail", ""));
                    salon_name.         setText(settings.getString("salonName", ""));
                    salon_address.      setText(settings.getString("salonAddr", ""));
                    salon_phone.        setText(settings.getString("salonPhone", ""));

                }

                if (settings.contains("absPath") && settings.getString("absPath", "").length() > 10) {
                    File imgFile = new File(settings.getString("absPath", ""));
                    if (imgFile.exists()) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        myImage.setImageBitmap(resize(myBitmap));
                    }else{
                        if(photo!=null) {
                            myImage.setImageBitmap(resize(photo));
                        }

                    }
                }
            }
            if (barberOrSalon==ViewPagerAdapter.SALON) {
                TextView newSalonName = (TextView) findViewById(R.id.new_salon_name);
                TextView newSalonPhone = (TextView) findViewById(R.id.new_salon_phone);
                TextView new_salon_plus38 = (TextView) findViewById(R.id.new_salon_plus38);
                TextView newSalonAddress = (TextView) findViewById(R.id.new_salon_address);
                TextView newSalonEmail = (TextView) findViewById(R.id.new_salon_email);
                TextView comments = (TextView) findViewById(R.id.new_salon_comments);
                final ImageButton myLogo = (ImageButton) findViewById(R.id.salon_logo);
                myLogo.setOnClickListener(null);
                newSalonName.setTypeface(tf);
                newSalonPhone.setTypeface(tf);
                newSalonAddress.setTypeface(tf);
                comments.setTypeface(tf);
                new_salon_plus38.setTypeface(tf);
                newSalonEmail.setTypeface(tf);

                if(readOnly&&jsonBarber!=null){
                    comments.setEnabled(false);
                    comments.setKeyListener(null);
                    newSalonAddress.setEnabled(false);
                    newSalonAddress.setKeyListener(null);
                    newSalonEmail.setEnabled(false);
                    newSalonEmail.setKeyListener(null);
                    newSalonName.setEnabled(false);
                    newSalonName.setKeyListener(null);
                    newSalonPhone.setEnabled(false);
                    newSalonPhone.setKeyListener(null);


                    Button callback =(Button) findViewById(R.id.new_salon_callback);
                    callback.setVisibility(View.VISIBLE);
                    callback.setTypeface(tf);
                    comments.setText(jsonBarber.getString("comments"));
                    newSalonEmail.setText(jsonBarber.getString("email"));
                    newSalonAddress.setText(jsonBarber.getString("salon_address"));
                    newSalonPhone.setText(jsonBarber.getString("phone_number").substring(3));
                    newSalonName.setText(jsonBarber.getString("name"));
                    new AsyncTask<Void, Void, Void>() {
                        public Bitmap[] bitmap=new Bitmap[1];

                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                URL url = new URL(UPLOADED_IMAGE_PATH+jsonBarber.getString("photo_path"));
                               //Log.i("zzz","Salon_path="+url.getPath());
                                InputStream in = url.openStream();
                                bitmap[0] = decodeStream(in);
                            } catch (Exception e) {e.printStackTrace();}
                            return null;
                        }
                        @Override
                        protected void onPostExecute(Void result) {
                            if (bitmap[0] != null) {
                                myLogo.setImageBitmap(resize(bitmap[0]));
                                //Log.i("zzz", "image="+bitmap[0].getByteCount());
                            }
                        }
                    }.execute();


                }else {
                    comments.setText(settings.getString("comments", ""));
                    newSalonEmail.setText(settings.getString("newSalonEmail", ""));
                    newSalonAddress.setText(settings.getString("newSalonAddress", ""));
                    newSalonPhone.setText(settings.getString("newSalonPhone", ""));
                    newSalonName.setText(settings.getString("newSalonName", ""));


                    if (settings.contains("absSalonPath") && settings.getString("absSalonPath", "").length() > 10) {
                        File imgFile = new File(settings.getString("absSalonPath", ""));
                        if (imgFile.exists()) {
                            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                            myLogo.setImageBitmap(resize(myBitmap));
                        } else {
                            if (photo != null) {

                                myLogo.setImageBitmap(resize(photo));
                            }

                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void callback(View v) {
        try {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings.getString("clientPhone", "").length() == 10) {
                JSONObject jo = new JSONObject();
                jo.put("dataType", "callback");
                jo.put("barber_phone", jsonBarber.getString("phone_number"));
                jo.put("client_phone", "+38"+settings.getString("clientPhone", ""));
                new JSONParser(jo, this).execute();

            }else{
                showToast("зарегимтрируйтесь как клиент");
            }
            Log.i("zzz", "message sent " + jsonBarber.getString("phone_number")
                    + "|" + settings.getString("clientPhone", ""));
        }catch(JSONException e){
                e.printStackTrace();
            }
    }
    private void saveGeneral(){
        if(!readOnly) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();

            if (barberOrSalon == ViewPagerAdapter.BARBER) {
                TextView tvBarberName = (TextView) findViewById(R.id.barber_name);
                TextView tvBarberSecondName = (TextView) findViewById(R.id.barber_second_name);
                TextView tvBarberPhoneNumber = (TextView) findViewById(R.id.barber_phone_number);
                TextView tvBarberEmail = (TextView) findViewById(R.id.barber_email);
                TextView tvSalonName = (TextView) findViewById(R.id.salon_name);
                TextView tvSalonAddr = (TextView) findViewById(R.id.salon_address);
                TextView tvSalonPhone = (TextView) findViewById(R.id.salon_phone);
                CheckBox chIsViber = (CheckBox) findViewById(R.id.isViber);
                Log.i("photo", "photo before save " + uploadedFilePath);
                if (uploadedFilePath != null)
                    editor.putString("uploadedFilePath", uploadedFilePath);
                //editor.putBoolean("barber_saved", true);
                editor.putString("barberName", cleanString(tvBarberName.getText().toString()));
                editor.putString("barberPhone", cleanString(tvBarberPhoneNumber.getText().toString()));
                editor.putString("barberSecondName", cleanString(tvBarberSecondName.getText().toString()));
                editor.putString("barberEmail", cleanString(tvBarberEmail.getText().toString()));
                editor.putString("salonName", cleanString(tvSalonName.getText().toString()));
                editor.putString("salonAddr", cleanString(tvSalonAddr.getText().toString()));
                editor.putString("salonPhone", cleanString(tvSalonPhone.getText().toString()));
                editor.putBoolean("isViber", chIsViber.isChecked());
                editor.commit();
                Log.i("photo", "saved photo =" + settings.getString("uploadedFilePath", "ЖОПА"));
            }
            if (barberOrSalon == ViewPagerAdapter.SALON) {
                TextView tvSalonName = (TextView) findViewById(R.id.new_salon_name);
                TextView tvSalonPhone = (TextView) findViewById(R.id.new_salon_phone);
                TextView tvSalonEmail = (TextView) findViewById(R.id.new_salon_email);
                TextView tvSalonAddress = (TextView) findViewById(R.id.new_salon_address);
                TextView tvComments = (TextView) findViewById(R.id.new_salon_comments);
                editor.putString("newSalonName", cleanString(tvSalonName.getText().toString()));
                editor.putString("newSalonPhone", cleanString(tvSalonPhone.getText().toString()));
                editor.putString("newSalonEmail", cleanString(tvSalonEmail.getText().toString()));
                editor.putString("newSalonAddress", cleanString(tvSalonAddress.getText().toString()));
                editor.putString("comments", cleanString(tvComments.getText().toString()));
                if (uploadedFilePath != null)
                    editor.putString("uploadedSalonFilePath", uploadedFilePath);
                editor.commit();
            }
        }
    }
    private void saveShedule(){
        if(!readOnly) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();

            tvMonOpenTime = (TextView) findViewById(R.id.mondayOpenTime);
            tvMonFinishTime = (TextView) findViewById(R.id.mondayFinishTime);
            tvTueOpenTime = (TextView) findViewById(R.id.tuesdayOpenTime);
            tvTueFinishTime = (TextView) findViewById(R.id.tuesdayFinishTime);
            tvWedOpenTime = (TextView) findViewById(R.id.wednesdayOpenTime);
            tvWedFinishTime = (TextView) findViewById(R.id.wednesdayFinishTime);
            tvThuOpenTime = (TextView) findViewById(R.id.thursdayOpenTime);
            tvThuFinishTime = (TextView) findViewById(R.id.thursdayFinishTime);
            tvFriOpenTime = (TextView) findViewById(R.id.fridayOpenTime);
            tvFriFinishTime = (TextView) findViewById(R.id.fridayOpenTime);
            tvSatOpenTime = (TextView) findViewById(R.id.saturdayOpenTime);
            tvSatFinishTime = (TextView) findViewById(R.id.saturdayFinishTime);
            tvSunOpenTime = (TextView) findViewById(R.id.sundayOpenTime);
            tvSunFinishTime = (TextView) findViewById(R.id.sundayFinishTime);
            tvOddOpenTime = (TextView) findViewById(R.id.oddOpenTime);
            tvOddFinishTime = (TextView) findViewById(R.id.oddFinishTime);
            tvEvenOpenTime = (TextView) findViewById(R.id.evenOpenTime);
            tvEvenFinishTime = (TextView) findViewById(R.id.evenFinishTime);

            chIsMonday = (CheckBox) findViewById(R.id.isMonday);
            chIsTuesday = (CheckBox) findViewById(R.id.isTuesday);
            chIsWednesday = (CheckBox) findViewById(R.id.isWednesday);
            chIsThursday = (CheckBox) findViewById(R.id.isThursday);
            chIsFriday = (CheckBox) findViewById(R.id.isFriday);
            chIsSaturday = (CheckBox) findViewById(R.id.isSaturday);
            chIsSunday = (CheckBox) findViewById(R.id.isSunday);
            chIsOdd = (CheckBox) findViewById(R.id.isOdd);
            chIsEven = (CheckBox) findViewById(R.id.isEven);
            String tmp;
            int monOpenTime;
            tmp = cleanString(tvMonOpenTime.getText().toString());
            if (tmp.length() > 0) monOpenTime = Integer.parseInt(tmp);
            else monOpenTime = 9;
            int monFinishTime;
            tmp = cleanString(tvMonFinishTime.getText().toString());
            if (tmp.length() > 0) monFinishTime = Integer.parseInt(tmp);
            else monFinishTime = 18;
            int tueOpenTime;
            tmp = cleanString(tvTueOpenTime.getText().toString());
            if (tmp.length() > 0) tueOpenTime = Integer.parseInt(tmp);
            else tueOpenTime = 9;
            int tueFinishTime;
            tmp = cleanString(tvTueFinishTime.getText().toString());
            if (tmp.length() > 0) tueFinishTime = Integer.parseInt(tmp);
            else tueFinishTime = 18;
            int wedOpenTime;
            tmp = cleanString(tvWedOpenTime.getText().toString());
            if (tmp.length() > 0) wedOpenTime = Integer.parseInt(tmp);
            else wedOpenTime = 9;
            int wedFinishTime;
            tmp = cleanString(tvWedFinishTime.getText().toString());
            if (tmp.length() > 0) wedFinishTime = Integer.parseInt(tmp);
            else wedFinishTime = 18;
            int thuOpenTime;
            tmp = cleanString(tvThuOpenTime.getText().toString());
            if (tmp.length() > 0) thuOpenTime = Integer.parseInt(tmp);
            else thuOpenTime = 9;
            int thuFinishTime;
            tmp = cleanString(tvThuFinishTime.getText().toString());
            if (tmp.length() > 0) thuFinishTime = Integer.parseInt(tmp);
            else thuFinishTime = 18;
            int friOpenTime;
            tmp = cleanString(tvFriOpenTime.getText().toString());
            if (tmp.length() > 0) friOpenTime = Integer.parseInt(tmp);
            else friOpenTime = 9;
            int friFinishTime;
            tmp = cleanString(tvFriFinishTime.getText().toString());
            if (tmp.length() > 0) friFinishTime = Integer.parseInt(tmp);
            else friFinishTime = 18;
            int satOpenTime;
            tmp = cleanString(tvSatOpenTime.getText().toString());
            if (tmp.length() > 0) satOpenTime = Integer.parseInt(tmp);
            else satOpenTime = 9;
            int satFinishTime;
            tmp = cleanString(tvSatFinishTime.getText().toString());
            if (tmp.length() > 0) satFinishTime = Integer.parseInt(tmp);
            else satFinishTime = 18;
            int sunOpenTime;
            tmp = cleanString(tvSunOpenTime.getText().toString());
            if (tmp.length() > 0) sunOpenTime = Integer.parseInt(tmp);
            else sunOpenTime = 9;
            int sunFinishTime;
            tmp = cleanString(tvSunFinishTime.getText().toString());
            if (tmp.length() > 0) sunFinishTime = Integer.parseInt(tmp);
            else sunFinishTime = 18;
            int oddOpenTime;
            tmp = cleanString(tvOddOpenTime.getText().toString());
            if (tmp.length() > 0) oddOpenTime = Integer.parseInt(tmp);
            else oddOpenTime = 9;
            int oddFinishTime;
            tmp = cleanString(tvOddFinishTime.getText().toString());
            if (tmp.length() > 0) oddFinishTime = Integer.parseInt(tmp);
            else oddFinishTime = 18;
            int evenOpenTime;
            tmp = cleanString(tvEvenOpenTime.getText().toString());
            if (tmp.length() > 0) evenOpenTime = Integer.parseInt(tmp);
            else evenOpenTime = 9;
            int evenFinishTime;
            tmp = cleanString(tvEvenFinishTime.getText().toString());
            if (tmp.length() > 0) evenFinishTime = Integer.parseInt(tmp);
            else evenFinishTime = 18;

            editor.putBoolean("isMonday", chIsMonday.isChecked());
            editor.putBoolean("isWednesday", chIsWednesday.isChecked());
            editor.putBoolean("isTuesday", chIsTuesday.isChecked());
            editor.putBoolean("isThursday", chIsThursday.isChecked());
            editor.putBoolean("isFriday", chIsFriday.isChecked());
            editor.putBoolean("isSaturday", chIsSaturday.isChecked());
            editor.putBoolean("isSunday", chIsSunday.isChecked());
            editor.putBoolean("isOdd", chIsOdd.isChecked());
            editor.putBoolean("isEven", chIsEven.isChecked());
            editor.putInt("monOpenTime", monOpenTime);
            editor.putInt("monFinishTime", monFinishTime);
            editor.putInt("tueOpenTime", tueOpenTime);
            editor.putInt("tueFinishTime", tueFinishTime);
            editor.putInt("wedOpenTime", wedOpenTime);
            editor.putInt("wedFinishTime", wedFinishTime);
            editor.putInt("thuOpenTime", thuOpenTime);
            editor.putInt("thuFinishTime", thuFinishTime);
            editor.putInt("friOpenTime", friOpenTime);
            editor.putInt("friFinishTime", friFinishTime);
            editor.putInt("satOpenTime", satOpenTime);
            editor.putInt("satFinishTime", satFinishTime);
            editor.putInt("sunOpenTime", sunOpenTime);
            editor.putInt("sunFinishTime", sunFinishTime);
            editor.putInt("oddOpenTime", satOpenTime);
            editor.putInt("oddFinishTime", satFinishTime);
            editor.putInt("evenOpenTime", sunOpenTime);
            editor.putInt("evenFinishTime", sunFinishTime);
            editor.commit();
        }
    }
    private void setUpShedule(SharedPreferences settings) {
        try {
            CheckBox isMonday = (CheckBox) findViewById(R.id.isMonday);
            isMonday.setTypeface(tf);
            CheckBox isTuesday = (CheckBox) findViewById(R.id.isTuesday);
            isTuesday.setTypeface(tf);
            CheckBox isWednesday = (CheckBox) findViewById(R.id.isWednesday);
            isWednesday.setTypeface(tf);
            CheckBox isThursday = (CheckBox) findViewById(R.id.isThursday);
            isThursday.setTypeface(tf);
            CheckBox isFriday = (CheckBox) findViewById(R.id.isFriday);
            isFriday.setTypeface(tf);
            CheckBox isSaturday = (CheckBox) findViewById(R.id.isSaturday);
            isSaturday.setTypeface(tf);
            CheckBox isSunday = (CheckBox) findViewById(R.id.isSunday);
            isSunday.setTypeface(tf);
            CheckBox isEven = (CheckBox) findViewById(R.id.isEven);
            isEven.setTypeface(tf);
            CheckBox isOdd = (CheckBox) findViewById(R.id.isOdd);
            isOdd.setTypeface(tf);

            TextView oddOpenTime = (TextView) findViewById(R.id.oddOpenTime);
            oddOpenTime.setTypeface(tf);
            TextView oddFinishTime = (TextView) findViewById(R.id.oddFinishTime);
            oddFinishTime.setTypeface(tf);
            TextView evenFinishTime = (TextView) findViewById(R.id.evenFinishTime);
            evenFinishTime.setTypeface(tf);
            TextView evenOpenTime = (TextView) findViewById(R.id.evenOpenTime);
            evenOpenTime.setTypeface(tf);
            TextView mondayOpenTime = (TextView) findViewById(R.id.mondayOpenTime);
            mondayOpenTime.setTypeface(tf);
            TextView tuesdayOpenTime = (TextView) findViewById(R.id.tuesdayOpenTime);
            tuesdayOpenTime.setTypeface(tf);
            TextView wednesdayOpenTime = (TextView) findViewById(R.id.wednesdayOpenTime);
            wednesdayOpenTime.setTypeface(tf);
            TextView thursdayOpenTime = (TextView) findViewById(R.id.thursdayOpenTime);
            thursdayOpenTime.setTypeface(tf);
            TextView fridayOpenTime = (TextView) findViewById(R.id.fridayOpenTime);
            fridayOpenTime.setTypeface(tf);
            TextView saturdayOpenTime = (TextView) findViewById(R.id.saturdayOpenTime);
            saturdayOpenTime.setTypeface(tf);
            TextView sundayOpenTime = (TextView) findViewById(R.id.sundayOpenTime);
            sundayOpenTime.setTypeface(tf);
            TextView mondayFinishTime = (TextView) findViewById(R.id.mondayFinishTime);
            mondayFinishTime.setTypeface(tf);
            TextView tuesdayFinishTime = (TextView) findViewById(R.id.tuesdayFinishTime);
            tuesdayFinishTime.setTypeface(tf);
            TextView wednesdayFinishTime = (TextView) findViewById(R.id.wednesdayFinishTime);
            wednesdayFinishTime.setTypeface(tf);
            TextView thursdayFinishTime = (TextView) findViewById(R.id.thursdayFinishTime);
            thursdayFinishTime.setTypeface(tf);
            TextView fridayFinishTime = (TextView) findViewById(R.id.fridayFinishTime);
            fridayFinishTime.setTypeface(tf);
            TextView saturdayFinishTime = (TextView) findViewById(R.id.saturdayFinishTime);
            saturdayFinishTime.setTypeface(tf);
            TextView sundayFinishTime = (TextView) findViewById(R.id.sundayFinishTime);
            sundayFinishTime.setTypeface(tf);
            if (readOnly && jsonBarber != null) {

                LinearLayout l = (LinearLayout) findViewById(R.id.sheduleGrid);
                l.removeAllViewsInLayout();
                TextView t = new TextView(l.getContext());
                t.setText(jsonBarber.getString("shedule_text"));
                t.setTypeface(tf);
                t.setTextColor(getResources().getColor(R.color.edit_text_color));
                t.setTextSize(getResources().getDimension(R.dimen.text_size));
                t.setMinLines(4);
                l.addView(t);
            } else {
                isMonday.setChecked(settings.getBoolean("isMonday", false));
                isTuesday.setChecked(settings.getBoolean("isTuesday", false));
                isWednesday.setChecked(settings.getBoolean("isWednesday", false));
                isThursday.setChecked(settings.getBoolean("isThursday", false));
                isFriday.setChecked(settings.getBoolean("isFriday", false));
                isSaturday.setChecked(settings.getBoolean("isSaturday", false));
                isSaturday.setChecked(settings.getBoolean("isSaturday", false));
                isSunday.setChecked(settings.getBoolean("isSunday", false));
                isEven.setChecked(settings.getBoolean("isEven", true));
                isOdd.setChecked(settings.getBoolean("isOdd", true));

                oddOpenTime.setText(settings.getInt("oddOpenTime", 9));
                oddFinishTime.setText(settings.getString("oddFinishTime", ""));
                evenFinishTime.setText(settings.getString("evenFinishTime", ""));
                evenOpenTime.setText(settings.getString("evenOpenTime", ""));
                mondayOpenTime.setText(settings.getString("monOpenTime", ""));
                tuesdayOpenTime.setText(settings.getString("tueOpenTime", ""));
                wednesdayOpenTime.setText(settings.getString("wedOpenTime", ""));
                thursdayOpenTime.setText(settings.getString("thuOpenTime", ""));
                fridayOpenTime.setText(settings.getString("friOpenTime", ""));
                saturdayOpenTime.setText(settings.getString("satOpenTime", ""));
                sundayOpenTime.setText(settings.getString("sunOpenTime", ""));
                mondayFinishTime.setText(settings.getString("monFinishTime", ""));
                tuesdayFinishTime.setText(settings.getString("tueFinishTime", ""));
                wednesdayFinishTime.setText(settings.getString("wedFinishTime", ""));
                thursdayFinishTime.setText(settings.getString("thuFinishTime", ""));
                fridayFinishTime.setText(settings.getString("friFinishTime", ""));
                saturdayFinishTime.setText(settings.getString("satFinishTime", ""));
                sundayFinishTime.setText(settings.getString("sunFinishTime", ""));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveQualification(){
        if(!readOnly) {
            //Log.i("save", "\nsaveQualification\n");
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();

            CheckBox chIsBarber = (CheckBox) findViewById(R.id.isBarber);
            CheckBox chIsStylist = (CheckBox) findViewById(R.id.isStylist);
            CheckBox chIsMaster = (CheckBox) findViewById(R.id.isMaster);
            CheckBox chIsManCut = (CheckBox) findViewById(R.id.isManCut);
            CheckBox chIsWomanCut = (CheckBox) findViewById(R.id.isWomanCut);
            CheckBox chIsEvening = (CheckBox) findViewById(R.id.isEvening);
            CheckBox chIsColor = (CheckBox) findViewById(R.id.isColor);
            CheckBox chIsHighlight = (CheckBox) findViewById(R.id.isHighLight);
            if (barberOrSalon == ViewPagerAdapter.BARBER) {
                editor.putBoolean("isBarber", chIsBarber.isChecked());
                editor.putBoolean("isStylist", chIsStylist.isChecked());
                editor.putBoolean("isMaster", chIsMaster.isChecked());
            }
            editor.putBoolean("isManCut", chIsManCut.isChecked());
            editor.putBoolean("isWomanCut", chIsWomanCut.isChecked());
            editor.putBoolean("isEvening", chIsEvening.isChecked());
            editor.putBoolean("isColor", chIsColor.isChecked());
            editor.putBoolean("isHighlight", chIsHighlight.isChecked());
            editor.commit();
        }
    }
    private void setUpQualification(SharedPreferences settings) {
        try {
            TextView specializationText = (TextView) findViewById(R.id.specializationText);
            specializationText.setTypeface(tf);
            CheckBox isWomanCut  = (CheckBox) findViewById(R.id.isWomanCut);
            CheckBox isManCut    = (CheckBox) findViewById(R.id.isManCut);
            CheckBox isEvening   = (CheckBox) findViewById(R.id.isEvening);
            CheckBox isColor     = (CheckBox) findViewById(R.id.isColor);
            CheckBox isHighLight = (CheckBox) findViewById(R.id.isHighLight);
            CheckBox isBarber    = (CheckBox) findViewById(R.id.isBarber);
            CheckBox isStylist   = (CheckBox) findViewById(R.id.isStylist);
            CheckBox isMaster    = (CheckBox) findViewById(R.id.isMaster);
            if(barberOrSalon==ViewPagerAdapter.BARBER){
                if(readOnly&&jsonBarber!=null){
                    LinearLayout sp = (LinearLayout) findViewById(R.id.specializationGrid);
                    sp.removeAllViewsInLayout();
                    JSONArray specsJ=jsonBarber.getJSONArray("specializations");
                    String[] specs=new String[specsJ.length()];
                    for(int j=0;j<specsJ.length();j++){
                        CheckBox tmp = new CheckBox(sp.getContext());
                        tmp.setChecked(true);
                        tmp.setTypeface(tf);
                        tmp.setTextColor(getResources().getColor(R.color.edit_text_color));
                        tmp.setTextSize(getResources().getDimension(R.dimen.text_size));
                        tmp.setText((CharSequence) specsJ.get(j));
                        sp.addView(tmp);
                        //specs[j]=(String)specsJ.get(j);
                    }
                    LinearLayout q = (LinearLayout) findViewById(R.id.qualificationGrid);
                    TextView label = (TextView)q.getChildAt(0);
                    q.removeAllViewsInLayout();
                    q.addView(label);
                    CheckBox tmp = new CheckBox(q.getContext());
                    tmp.setChecked(true);
                    tmp.setTypeface(tf);
                    tmp.setTextColor(getResources().getColor(R.color.edit_text_color));
                    tmp.setTextSize(getResources().getDimension(R.dimen.text_size));
                    tmp.setText(jsonBarber.getString("qualification"));
                    q.addView(tmp);


                }else {
                    TextView qualification = (TextView) findViewById(R.id.qualification);
                    qualification.setTypeface(tf);

                    isBarber.setTypeface(tf);
                    isBarber.setChecked(settings.getBoolean("isBarber", false));
                    isStylist.setTypeface(tf);
                    isStylist.setChecked(settings.getBoolean("isStylist", false));
                    isMaster.setTypeface(tf);
                    isMaster.setChecked(settings.getBoolean("isMaster", false));
                }
            }else {
                if (readOnly && jsonBarber != null) {
                    LinearLayout sp = (LinearLayout) findViewById(R.id.specializationGrid);
                    sp.removeAllViewsInLayout();
                    JSONArray specsJ=jsonBarber.getJSONArray("specializations");

                    for(int j=0;j<specsJ.length();j++){
                        CheckBox tmp = new CheckBox(sp.getContext());
                        tmp.setChecked(true);
                        tmp.setTypeface(tf);
                        tmp.setTextColor(getResources().getColor(R.color.edit_text_color));
                        tmp.setTextSize(getResources().getDimension(R.dimen.text_size));
                        tmp.setText((CharSequence) specsJ.get(j));
                        sp.addView(tmp);
                        //specs[j]=(String)specsJ.get(j);
                    }
                    LinearLayout main = (LinearLayout) findViewById(R.id.main_qualification_layout);
                    main.removeViewAt(0);
                } else {
                    LinearLayout l = (LinearLayout) findViewById(R.id.qualificationGrid);
                    l.setVisibility(View.INVISIBLE);
                    isBarber.setOnClickListener(null);
                    isBarber.setChecked(false);
                    isMaster.setOnClickListener(null);
                    isMaster.setChecked(false);
                    isStylist.setOnClickListener(null);
                    isStylist.setChecked(false);
                }
            }
            isWomanCut.setTypeface(tf);
            isWomanCut.setChecked(settings.getBoolean("isWomanCut", false));
            isManCut.setTypeface(tf);
            isManCut.setChecked(settings.getBoolean("isManCut", false));
            isEvening.setTypeface(tf);
            isEvening.setChecked(settings.getBoolean("isEvening", false));
            isColor.setTypeface(tf);
            isColor.setChecked(settings.getBoolean("isColor", false));
            isHighLight.setTypeface(tf);
            isHighLight.setChecked(settings.getBoolean("isHighLight", false));
        }catch (Exception e ){
            e.printStackTrace();
        }

    }





    private void savePrices(){
        if(!readOnly) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            TextView manCutPrice = (TextView) findViewById(R.id.prices_man_cut_price);
            TextView womanCutPrice = (TextView) findViewById(R.id.prices_woman_cut_price);
            TextView hairdoPrice = (TextView) findViewById(R.id.prices_hairdo_price);
            TextView colorizePrice = (TextView) findViewById(R.id.prices_colorize_price);
            editor.putString("manCutPrice", cleanString(manCutPrice.getText().toString()));
            editor.putString("womanCutPrice", cleanString(womanCutPrice.getText().toString()));
            editor.putString("hairdoPrice", cleanString(hairdoPrice.getText().toString()));
            editor.putString("colorizePrice", cleanString(colorizePrice.getText().toString()));
            editor.commit();
        }
    }
    private void setUpPrices(SharedPreferences settings) {
        try {
            TextView prices_base_cut_text = (TextView) findViewById(R.id.prices_base_cut_text);
            TextView prices_colorize_price = (TextView) findViewById(R.id.prices_colorize_price);
            TextView prices_colorize_text = (TextView) findViewById(R.id.prices_colorize_text);
            TextView prices_hairdo_price = (TextView) findViewById(R.id.prices_hairdo_price);
            TextView prices_hairdo_text = (TextView) findViewById(R.id.prices_hairdo_text);
            TextView prices_man_cut_price = (TextView) findViewById(R.id.prices_man_cut_price);
            TextView prices_man_cut_text = (TextView) findViewById(R.id.prices_man_cut_text);
            TextView prices_woman_cut_price = (TextView) findViewById(R.id.prices_woman_cut_price);
            TextView prices_woman_cut_rext = (TextView) findViewById(R.id.prices_woman_cut_rext);
            TextView prices_grn1 = (TextView) findViewById(R.id.prices_grn1);
            TextView prices_grn2 = (TextView) findViewById(R.id.prices_grn2);
            TextView prices_grn3 = (TextView) findViewById(R.id.prices_grn3);
            TextView prices_grn4 = (TextView) findViewById(R.id.prices_grn4);

            prices_base_cut_text.setTypeface(tf);
            prices_colorize_price.setTypeface(tf);
            prices_colorize_text.setTypeface(tf);
            prices_hairdo_price.setTypeface(tf);
            prices_hairdo_text.setTypeface(tf);
            prices_man_cut_price.setTypeface(tf);
            prices_man_cut_text.setTypeface(tf);
            prices_woman_cut_price.setTypeface(tf);
            prices_woman_cut_rext.setTypeface(tf);
            prices_grn1.setTypeface(tf);
            prices_grn2.setTypeface(tf);
            prices_grn3.setTypeface(tf);
            prices_grn4.setTypeface(tf);


            if(readOnly&&jsonBarber!=null){
                prices_colorize_price.setEnabled(false);
                prices_hairdo_price.setEnabled(false);
                prices_man_cut_price.setEnabled(false);
                prices_woman_cut_price.setEnabled(false);

                prices_colorize_price.setHint("");
                prices_hairdo_price.setHint("");
                prices_man_cut_price.setHint("");
                prices_woman_cut_price.setHint("");


                JSONObject prices=jsonBarber.getJSONObject("prices");
                prices_colorize_price.setText(prices.getString("colorize"));
                prices_hairdo_price.setText(prices.getString("hairdo"));
                prices_man_cut_price.setText(prices.getString("man_cut"));
                prices_woman_cut_price.setText(prices.getString("woman_cut"));
                LinearLayout p = (LinearLayout) findViewById(R.id.prices_layout);
                p.removeView(findViewById(R.id.prices_ok_button));

            }else {
                prices_colorize_price.setText(settings.getString("colorizePrice", ""));
                prices_hairdo_price.setText(settings.getString("hairdoPrice", ""));
                prices_man_cut_price.setText(settings.getString("manCutPrice", ""));
                prices_woman_cut_price.setText(settings.getString("womanCutPrice", ""));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void checkNewBarberData(View v) {
        savePrices();
        JSONObject jo = new JSONObject();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        String errorStr = "";
        try {

            if(barberOrSalon==ViewPagerAdapter.BARBER){
                jo.put("dataType", "newBarber");

                String barberName = settings.getString("barberName","");
                if (barberName.length() > 0) jo.put("barberName", barberName);
                else errorStr += "Введите Ваше имя\n";

                String barberPhone = settings.getString("barberPhone","");
                if (barberPhone.length() > 0) jo.put("barberPhone", "+38" + barberPhone);
                else errorStr += "Введите Ваш номер телефона\n";

                String barberSecondName = settings.getString("barberSecondName","");
                if (barberSecondName.length() > 0) jo.put("barberSecondName", barberSecondName);

                String barberEmail = settings.getString("barberEmail","");
                if (barberEmail.length() > 0) jo.put("barberEmail", barberEmail);
                //else errorStr += "Введите Ваш e-mail\n";

                if (uploadedFilePath != null) jo.put("uploadedFilePath", uploadedFilePath);

                String salonName = settings.getString("salonName","");
                if (salonName.length() > 0) jo.put("salonName", salonName);

                String salonAddr = settings.getString("salonAddr","");
                if (salonAddr.length() > 0) jo.put("salonAddr", salonAddr);
                else errorStr += "Введите Ваш адрес\n";

                String salonPhone = settings.getString("salonPhone","");
                if (salonPhone.length() > 0) jo.put("salonPhone", salonPhone);

                Boolean isViber = settings.getBoolean("isViber",false);
                jo.put("isViber", isViber);
            }

            if(barberOrSalon==ViewPagerAdapter.SALON){
                jo.put("dataType", "newSalon");

                String newSalonName = settings.getString("newSalonName","");
                if (newSalonName.length() > 0) jo.put("newSalonName", newSalonName);
                else errorStr += "Введите название салона\n";

                String newSalonPhone = settings.getString("newSalonPhone","");
                if (newSalonPhone.length() > 0) jo.put("newSalonPhone", "+38" + newSalonPhone);
                else errorStr += "Введите номер телефона\n";

                String newSalonEmail = settings.getString("newSalonEmail","");
                if (newSalonEmail.length() > 0) jo.put("newSalonEmail", newSalonEmail);

                String newSalonLogo = settings.getString("uploadedSalonFilePath","");
                if (newSalonLogo != "") jo.put("uploadedSalonFilePath", newSalonLogo);

                String comments = settings.getString("comments","");
                if (comments != "") jo.put("comments", comments);

                String newSalonAddress = settings.getString("newSalonAddress","");
                if (newSalonAddress.length() > 0) jo.put("newSalonAddress", newSalonAddress);
                else errorStr += "Введите Ваш адрес\n";
            }


            jo.put("isMonday",    settings.getBoolean("isMonday", false));
            jo.put("isTuesday",   settings.getBoolean("isTuesday", false));
            jo.put("isWednesday", settings.getBoolean("isWednesday", false));
            jo.put("isThursday",  settings.getBoolean("isThursday", false));
            jo.put("isFriday",    settings.getBoolean("isFriday",false));
            jo.put("isSaturday",  settings.getBoolean("isSaturday", false));
            jo.put("isSunday",    settings.getBoolean("isSunday", false));
            jo.put("isOdd",       settings.getBoolean("isOdd", true));
            jo.put("isEven",      settings.getBoolean("isEven", true));

            jo.put("monOpenTime",   settings.getInt("monOpenTime", 9));
            jo.put("monFinishTime", settings.getInt("monFinishTime",18));
            jo.put("tueOpenTime",   settings.getInt("tueOpenTime",   9));
            jo.put("tueFinishTime", settings.getInt("tueFinishTime",18));
            jo.put("wedOpenTime", settings.getInt("wedOpenTime", 9));
            jo.put("wedFinishTime", settings.getInt("wedFinishTime", 18));
            jo.put("thuOpenTime", settings.getInt("thuOpenTime", 9));
            jo.put("thuFinishTime", settings.getInt("thuFinishTime", 18));
            jo.put("friOpenTime", settings.getInt("friOpenTime", 9));
            jo.put("friFinishTime", settings.getInt("friFinishTime", 18));
            jo.put("satOpenTime", settings.getInt("satOpenTime", 9));
            jo.put("satFinishTime", settings.getInt("satFinishTime", 18));
            jo.put("sunOpenTime", settings.getInt("sunOpenTime", 9));
            jo.put("sunFinishTime", settings.getInt("sunFinishTime", 18));
            jo.put("oddOpenTime",   settings.getInt("oddOpenTime",   9));
            jo.put("oddFinishTime", settings.getInt("oddFinishTime", 18));
            jo.put("evenOpenTime", settings.getInt("evenOpenTime", 9));
            jo.put("evenFinishTime",settings.getInt("evenFinishTime",18));

            jo.put("manCutPrice", Integer.parseInt(settings.getString("manCutPrice", "")));
            jo.put("womanCutPrice",Integer.parseInt(settings.getString("womanCutPrice","")));
            jo.put("colorizePrice", Integer.parseInt(settings.getString("colorizePrice", "")));
            jo.put("hairdoPrice",Integer.parseInt(settings.getString("hairdoPrice","")));

            if (settings.getBoolean("isBarber", false))
                jo.put("qualification", "barber");
            else if (settings.getBoolean("isStylist", false))
                jo.put("qualification", "stylist");
            else if (settings.getBoolean("isMaster", false))
                jo.put("qualification", "master");

            jo.put("isManCut", settings.getBoolean("isManCut", false));
            jo.put("isWomanCut",   settings.getBoolean("isWomanCut", false));
            jo.put("isEvening", settings.getBoolean("isEvening", false));
            jo.put("isColor",      settings.getBoolean("isColor", false));
            jo.put("isHighlight", settings.getBoolean("isHighlight", false));

            if (errorStr.length() > 0) {
                Toast.makeText(this, errorStr, Toast.LENGTH_SHORT).show();
            } else {
                // lets add some headers (nested JSON object)
                JSONObject header = new JSONObject();
                header.put("devicemodel", android.os.Build.MODEL); // Device model
                header.put("deviceVersion", android.os.Build.VERSION.RELEASE); // Device OS version
                header.put("language", Locale.getDefault().getISO3Language()); // Language
                jo.put("header", header);
                JSONParser mJSONParser = new JSONParser(jo,this);
                mJSONParser.execute();
                //showMap();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void showMap(){
        Intent intent = new Intent(NewBarberActivity.this, MapActivity.class);
        intent.putExtra("showNewObject", true);
        startActivity(intent);
    }

    public void checkShedule(View v){
        try {
            if (chIsEven.isChecked() || chIsOdd.isChecked()) {
                Log.v("enabling","enablingMon=true");
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
                Log.v("enabling","enablingMon=true");
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
    /*
    private void showPin(){
        setContentView(R.layout.enter_pin);
        currentViewId=R.layout.enter_pin;
        TextView pin = (TextView) findViewById(R.id.pin_code);
        pin.setTypeface(tf);
    }
    */
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

    public void gotPin(String res){
        if(res.length()==4)
            clientPin=res;
    }
    public void showToast(String txt){
        Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();
    }

    public String cleanString(String dirty) {
        return Html.fromHtml(dirty).toString();
    }

}