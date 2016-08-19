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
    private double mCurrentLat = 0.0;
    private double mCurrentLong = 0.0;
    private double mCurrentAlt = 0.0;
    private boolean mIsEnabled;

    public FakeLocationManager(Context context) {
        mContext = context;

        // Start fetch information from framework hacking
        initFirstTime();

        // If the previous
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
            return;
        } else {

        }
    }

    private void setLatitude(double lat) {

    }

    private void setLongitude(double lon) {

    }

    private void setAltitude(double alt) {

    }

    private void setAccuracy(double acc) {

    }

}
