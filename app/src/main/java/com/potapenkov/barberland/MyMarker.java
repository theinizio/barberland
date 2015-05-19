package com.potapenkov.barberland;

import android.graphics.Bitmap;

public class MyMarker
{
    private String mLabel;
    private String mIcon;
    private Double mLatitude;
    private Double mLongitude;
    private String mPhoneNumber;
    private String mShedule;
    private int    mQualification;
    private Bitmap mBitmap;
    private String mSpecializations[];

    public static final int mBARBER=1;
    public static final int mSTYLIST=2;
    public static final int mMASTER=3;
    public static final int mSALON=4;



    public MyMarker(String label, String icon, String phoneNumber, int qualification,
                    String[] specializations, String shedule, Double latitude, Double longitude)
    {
        this.mLabel = label;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mIcon = icon;
        this.mPhoneNumber = phoneNumber;
        this.mQualification=qualification;
        this.mSpecializations=specializations;
        this.mShedule=shedule;
    }

    
    public MyMarker(String label, String icon, Double latitude, Double longitude)
    {
        this.mLabel = label;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mIcon = icon;
   }


    public String getmShedule() {
        return mShedule;
    }

    public void setmShedule(String mShedule) {
        this.mShedule = mShedule;
    }


    public Bitmap getmBitmap() {
        return mBitmap;
    }

    public void setmBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
    }



    public int getmQualification() {
        return mQualification;
    }

    public void setmQualification(int mQualification) {
        this.mQualification = mQualification;
    }
    
    public String getPhoneNumber() {
		return mPhoneNumber;
	}


	public void setPhoneNumber(String phoneNumber) {
		this.mPhoneNumber = phoneNumber;
	}


	public String getmLabel()
    {
        return mLabel;
    }

    public void setmLabel(String mLabel)
    {
        this.mLabel = mLabel;
    }

    public String getmIcon()
    {
        return mIcon;
    }

    public void setmIcon(String icon)
    {
        this.mIcon = icon;
    }

    public Double getmLatitude()
    {
        return mLatitude;
    }

    public void setmLatitude(Double mLatitude)
    {
        this.mLatitude = mLatitude;
    }

    public Double getmLongitude()
    {
        return mLongitude;
    }

    public void setmLongitude(Double mLongitude)
    {
        this.mLongitude = mLongitude;
    }




    public String[] getmSpecializations() {
        return mSpecializations;
    }

    public void setmSpecializations(String[] mSpecializations) {
        this.mSpecializations = mSpecializations;
    }






}
