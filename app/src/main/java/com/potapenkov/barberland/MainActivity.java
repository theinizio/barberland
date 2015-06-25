package com.potapenkov.barberland;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class MainActivity extends Activity  {
    private int currentViewId=-1;
    public static final String PREFS_NAME = "barberlandSettings";
     @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_page);
        currentViewId=R.layout.start_page;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clearAll) {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            editor.commit();
            return true;
        }
        if (id == R.id.exit) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed(){

        switch (currentViewId){
            case R.layout.barber_or_salon:
                setContentView(R.layout.start_page);
                currentViewId=R.layout.start_page;
                break;
            case R.layout.start_page:
                super.onBackPressed();
                break;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        setContentView(currentViewId);
        initBarberOrSalon();

    }
    public void newClient(View v){
        //finish();
        Intent intent = new Intent(MainActivity.this, NewClientActivity.class);
        startActivity(intent);

    }

    public void newBarber(View v){
        //finish();
        Intent intent = new Intent(MainActivity.this, NewBarberActivity.class);
        intent.putExtra("barberOrSalon", ViewPagerAdapter.BARBER);
        startActivity(intent);
    }
    public void newSalon(View v){
        //finish();
        Intent intent = new Intent(MainActivity.this, NewBarberActivity.class);
        intent.putExtra("barberOrSalon", ViewPagerAdapter.SALON);
        startActivity(intent);
    }
    public void barberOrSalon(View v){
        setContentView(R.layout.barber_or_salon);
        currentViewId=R.layout.barber_or_salon;
        initBarberOrSalon();
    }

    private void initBarberOrSalon(){
        try {
            Typeface tf = Typeface.createFromAsset(getAssets(), "teslic.ttf");
            Button b1 = (Button) findViewById(R.id.barber_button);
            b1.setTypeface(tf);
            Button b2 = (Button) findViewById(R.id.salon_button);
            b2.setTypeface(tf);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }
    public static String cleanString(String dirty) {
        return Html.fromHtml(dirty).toString();
    }


    private boolean saveSharedPreferencesToFile(File dst) {
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref =
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            output.writeObject(pref.getAll());

            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (output != null) {
                    output.flush();
                    output.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile(File src) {
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
            }
            prefEdit.commit();
            res = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return res;
    }


}
