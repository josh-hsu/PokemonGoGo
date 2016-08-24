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

public class FakeLocation {
    public double latitude;
    public double longitude;
    public double altitude;
    public double accuracy;

    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;
    public static final int NORTHWEST = 4;
    public static final int WESTSOUTH = 5;
    public static final int SOUTHEAST = 6;
    public static final int EASTNORTH = 7;
    public static final int STAY = 8;

    public FakeLocation(double la, double lo, double alt, double acc) {
        latitude = la;
        longitude = lo;
        altitude = alt;
        accuracy = acc;
    }

    public String toString() {
        return "Latitude = " + latitude + ", longitude = " + longitude + ", altitude = " +
                altitude + ", accuracy = " + accuracy;
    }
}
