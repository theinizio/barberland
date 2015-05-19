package com.potapenkov.barberland;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

class SpinnerAdapter extends BaseAdapter
{
    private Context context;
    ArrayList<String> red;
    ArrayList<String> green;
    ArrayList<String> blue;
    ArrayList<String> colors;

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
        txv.setTextSize(context.getResources().getDimension(R.dimen.text_size));
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