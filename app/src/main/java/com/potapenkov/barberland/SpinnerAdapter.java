package com.potapenkov.barberland;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;

class SpinnerAdapter extends BaseAdapter
{
    private Context context;
    ArrayList<String> red;
    ArrayList<String> green;
    ArrayList<String> blue;
    ArrayList<String> colors;
    public  ArrayList<LatLng> coords;

    public SpinnerAdapter(Context context)
    {
        this.context=context;
        colors = new ArrayList<String>();
        red= new ArrayList<String>(Arrays.asList(context.getResources().getStringArray(R.array.metro_list_red)));
        green= new ArrayList<String>(Arrays.asList(context.getResources().getStringArray(R.array.metro_list_green)));
        blue = new ArrayList<String>(Arrays.asList(context.getResources().getStringArray(R.array.metro_list_blue)));
        colors.addAll(red);
        colors.addAll(blue);
        colors.addAll(green);
        coords = new ArrayList<>();


        coords.add(new LatLng(50.464784, 30.355511));
        coords.add(new LatLng(50.455884, 30.364790));
        coords.add(new LatLng(50.457685, 30.391776));
        coords.add(new LatLng(50.458602, 30.405682));
        coords.add(new LatLng(50.458562, 30.420522));
        coords.add(new LatLng(50.454622, 30.445208));
        coords.add(new LatLng(50.450814, 30.466327));
        coords.add(new LatLng(50.441780, 30.488273));
        coords.add(new LatLng(50.444401, 30.506006));
        coords.add(new LatLng(50.445135, 30.518178));
        coords.add(new LatLng(50.447218, 30.524933));
        coords.add(new LatLng(50.444332, 30.545379));
        coords.add(new LatLng(50.441217, 30.559374));
        coords.add(new LatLng(50.445956, 30.576990));
        coords.add(new LatLng(50.451800, 30.598110));
        coords.add(new LatLng(50.455924, 30.612905));
        coords.add(new LatLng(50.459852, 30.630269));
        coords.add(new LatLng(50.464629, 30.645541));
        coords.add(new LatLng(50.522580, 30.498891));
        coords.add(new LatLng(50.512196, 30.498541));
        coords.add(new LatLng(50.501466, 30.498253));
        coords.add(new LatLng(50.486074, 30.497885));
        coords.add(new LatLng(50.473286, 30.505134));
        coords.add(new LatLng(50.465604, 30.514836));
        coords.add(new LatLng(50.458935, 30.524933));
        coords.add(new LatLng(50.450028, 30.524196));
        coords.add(new LatLng(50.439221, 30.516327));
        coords.add(new LatLng(50.432244, 30.516210));
        coords.add(new LatLng(50.420641, 30.520738));
        coords.add(new LatLng(50.412714, 30.524879));
        coords.add(new LatLng(50.404563, 30.516615));
        coords.add(new LatLng(50.397879, 30.510066));
        coords.add(new LatLng(50.392815, 30.485551));
        coords.add(new LatLng(50.382092, 30.477134));
        coords.add(new LatLng(50.376526, 30.468923));
        coords.add(new LatLng(50.367167, 30.454218));
        coords.add(new LatLng(50.476221, 30.430736));
        coords.add(new LatLng(50.473452, 30.448406));
        coords.add(new LatLng(50.462249, 30.482101));
        coords.add(new LatLng(50.448342, 30.513668));
        coords.add(new LatLng(50.437838, 30.520684));
        coords.add(new LatLng(50.436731, 30.531922));
        coords.add(new LatLng(50.427315, 30.538776));
        coords.add(new LatLng(50.418001, 30.544462));
        coords.add(new LatLng(50.401428, 30.560686));
        coords.add(new LatLng(50.394164, 30.604452));
        coords.add(new LatLng(50.395537, 30.616148));
        coords.add(new LatLng(50.398344, 30.634330));
        coords.add(new LatLng(50.401072, 30.652000));
        coords.add(new LatLng(50.403185, 30.665969));
        coords.add(new LatLng(50.403403, 30.682974));
        coords.add(new LatLng(50.409505, 30.695918));


    }
    @Override
    public int getCount()
    {
        return colors.size();
    }
    @Override
    public Object getItem(int arg0)
    {
        return colors.get(arg0);
    }
    @Override
    public long getItemId(int arg0)
    {
        return arg0;
    }
    @Override
    public View getView(int pos, View view, ViewGroup parent)
    {
        LayoutInflater inflater=LayoutInflater.from(context);
        view=inflater.inflate(android.R.layout.simple_spinner_dropdown_item, null);
        TextView txv=(TextView)view.findViewById(android.R.id.text1);
        String curText=colors.get(pos);
        txv.setBackgroundColor(getColorFromName(curText));
        txv.setTextColor(context.getResources().getColor(R.color.text));
        txv.setTextSize(21);
        txv.setTypeface(Typeface.createFromAsset(context.getAssets(), "teslic.ttf"));
        txv.setText(curText);
        return view;
    }
    private int getColorFromName(String item) {

        if (red.contains(item))return context.getResources().getColor(R.color.metro_red);
        if (green.contains(item))return context.getResources().getColor(R.color.metro_green);
        if (blue.contains(item))return context.getResources().getColor(R.color.metro_blue);
        return context.getResources().getColor(R.color.white);
    }
}