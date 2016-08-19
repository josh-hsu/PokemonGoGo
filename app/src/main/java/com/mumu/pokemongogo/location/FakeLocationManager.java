/*
 * Copyright (C) 2016 The Josh Tool Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mumu.pokemongogo.location;

import android.content.Context;
import android.util.Log;

import com.mumu.pokemongogo.R;

public class FakeLocationManager {
    private final static String TAG = "PokemonGoGo";
    private Context mContext;
    private double mCurrentLat = 25.0335;
    private double mCurrentLong = 121.5642;
    private double mCurrentAlt = 10.2;
    private double mCurrentAccuracy = 6.91;
    private static final double mPaceLat = 0.000051;
    private static final double mPaceLong = 0.000049;
    private static double mPaceLatShift = 0.000001;
    private static double mPaceLongShift = 0.000001;
    private boolean mIsEnabled;
    public FakeLocation mDefaultLocation;

    public FakeLocationManager(Context context, FakeLocation defaultLoc) {
        mContext = context;

        if (defaultLoc != null)
            mDefaultLocation = defaultLoc;
        else
            mDefaultLocation = new FakeLocation(25.0335, 121.5642, 10.2, 6.91); //this is the location of Taipei 101

        // Override location now, maybe in the future we should restore actual location
        setDefaultLocation(mDefaultLocation);

        // Start fetch information from framework hacking
        initFirstTime();
    }

    public void initFirstTime() {
        String enabled = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_enable));
        String currentLat = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_lat));
        String currentLong = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_long));
        String currentAlt = PropertyService.getSystemProperty(mContext.getString(R.string.property_fake_alt));

        mIsEnabled = enabled.equals("1");

        if (!currentLat.equals("")) {
            mCurrentLat = Double.parseDouble(currentLat);
        }

        if (!currentLong.equals("")) {
            mCurrentLong = Double.parseDouble(currentLong);
        }

        if (!currentAlt.equals("")) {
            mCurrentAlt = Double.parseDouble(currentAlt);
        }

        Log.d(TAG, "default: lat = " + mCurrentLat + ", long = " + mCurrentLong + ", alt = " + mCurrentAlt);
    }

    public void setEnable(boolean enable) {
        if (enable) {
            PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_enable), "1");
        } else {
            PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_enable), "0");
        }
    }

    public boolean getEnable() {
        return mIsEnabled;
    }

    private void setDefaultLocation(FakeLocation loc) {
        if (loc == null) {
            Log.e(TAG, "Cannot set default location null");
        } else {
            setLocation(loc);
        }
    }

    public void setLocation(FakeLocation loc) {
        Log.d(TAG, "Set location " + loc.toString());
        setLatitude(loc.latitude);
        setLongitude(loc.longitude);
        setAltitude(loc.altitude);
        setAccuracy(loc.accuracy);
    }

    public void walkPace(int direction) {
        // must introduce random variable
        controlRandomShift();

        if (direction == FakeLocation.EAST) {
            mCurrentLong -= (mPaceLong + mPaceLongShift);
            setLongitude(mCurrentLong);
        } else if (direction == FakeLocation.WEST) {
            mCurrentLong += (mPaceLong + mPaceLongShift);
            setLongitude(mCurrentLong);
        } else if (direction == FakeLocation.NORTH) {
            mCurrentLat += (mPaceLat + mPaceLatShift);
            setLatitude(mCurrentLat);
        } else if (direction == FakeLocation.SOUTH) {
            mCurrentLat -= (mPaceLat + mPaceLatShift);
            setLatitude(mCurrentLat);
        } else {
            Log.d(TAG, "Not supported direction");
        }

        setAccuracy(mCurrentAccuracy);
    }

    private void controlRandomShift() {
        // shift is controlled to be within -0.000001 ~ 0.000001
        double shift = Math.random() / 1000000 - 0.0000005;
        double accShift = Math.random() * 2 - 1;
        mPaceLatShift = mPaceLatShift + shift;
        mPaceLongShift = mPaceLongShift + shift;
        mCurrentAccuracy += accShift;

        if (mPaceLatShift > 0.000002 || mPaceLatShift < -0.000002)
            mPaceLatShift = 0.000001;

        if (mPaceLongShift > 0.000002 || mPaceLongShift < -0.000002)
            mPaceLongShift = 0.000001;

        if (mCurrentAccuracy > 9.9 || mCurrentAccuracy < 1.5)
            mCurrentAccuracy = 5.2;

        Log.d(TAG, "RANDOM lat = " + mPaceLatShift + ", long = " + mPaceLongShift);
    }

    private void setLatitude(double lat) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_lat), ""+lat);
    }

    private void setLongitude(double lon) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_long), ""+lon);
    }

    private void setAltitude(double alt) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_alt), ""+alt);
    }

    private void setAccuracy(double acc) {
        PropertyService.setSystemProperty(mContext.getString(R.string.property_fake_acc), ""+acc);
    }

}