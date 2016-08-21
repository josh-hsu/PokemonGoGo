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

import android.util.Log;

/*
 * Human Walk Simulator
 * When we walk on the street, we don't usually walk back and front and we tend
 * to walk in the same direction for a while.
 */
public class HumanWalkSimulator {
    private static final String TAG = "PokemonGoGo";
    private static final int MAX_DIR_COUNT = 150;
    private static final double MAX_POSSIBILITY = 0.92;
    private static final double STAY_POSSIBILITY = 0.1; // 10% chance that we stay for 1 second

    public static final int POLICY_AVERAGE = 0;
    public static final int POLICY_RANDOM = 1;
    public static final int POLICY_CONSTANT = 2;

    private int mCurrentDirection = FakeLocation.STAY;
    private int mCurrentCount = 0;
    private final int[] mPossibilityEntry = {FakeLocation.NORTH, FakeLocation.SOUTH,
            FakeLocation.WEST, FakeLocation.EAST};
    private double[] mPossibilityList = new double[4];
    private double[] mPossibilityStay = new double[2];

    public HumanWalkSimulator() {
        resetSimulator();
    }

    /*
     * after generateNewList, the next direction is saved in mCurrentDirection
     */
    public int getNextDirection() {
        return generateNewList();
    }

    public void resetSimulator() {
        mPossibilityList[0] = 0.25;
        mPossibilityList[1] = 0.25;
        mPossibilityList[2] = 0.25;
        mPossibilityList[3] = 0.25;

        mPossibilityStay[0] = 1 - STAY_POSSIBILITY; // index 0 also says NO
        mPossibilityStay[1] = STAY_POSSIBILITY;
    }

    /*
     * we set the current direction the highest possibility toward max dir count
     * the possibility gets smaller when the mCurrentCount gets bigger
     */
    private int generateNewList() {
        int roll = rollTheDice(mPossibilityList);
        int shouldStay = rollTheDice(mPossibilityStay);

        if (shouldStay == 1) {
            return FakeLocation.STAY;
        }

        if (roll != mCurrentDirection) {
            mCurrentCount = 0;
            mCurrentDirection = roll;
            setPossibility(mPossibilityList, roll, MAX_POSSIBILITY, POLICY_CONSTANT);
        } else {
            mCurrentCount++;
            setPossibility(mPossibilityList, roll,
                    MAX_POSSIBILITY * ((double)(MAX_DIR_COUNT - mCurrentCount) / (double)MAX_DIR_COUNT),
                    POLICY_RANDOM);
        }

        return mCurrentDirection;
    }

    private int rollTheDice(double[] list) {
        double random_seed = Math.random();
        double increment = 0;

        if (list.length < 2) {
            return 0;
        }

        for(int i = 0; i < list.length; i++) {
            increment += list[i];
            if (random_seed < increment) {
                return i;
            }
        }

        return list.length - 1;
    }

    private void setPossibility(double[] list, int index, double value, int policy) {
        switch (policy) {
            case POLICY_AVERAGE:
                setPossibilityAverage(list, index, value);
                break;
            case POLICY_RANDOM:
                break;
            case POLICY_CONSTANT:
                setPossibilityConstant(list, index, value);
                break;
            default:
                Log.w(TAG, "Set possibility with unrecognized policy");
        }
    }

    private void setPossibilityAverage(double[] list, int index, double value) {
        if (value < 0.0 || value > 1.0) {
            Log.e(TAG, "Set possibility of " + mPossibilityEntry[index] + " to " + value + " is not valid.");
        } else {
            double last_possibility = 1 - value;
            double old_bottom_value = 1 - list[index];
            for(int i = 0; i < list.length; i++) {
                if (i == index) {
                    list[i] = value;
                } else {
                    list[i] = list[i] * last_possibility / old_bottom_value;
                }
            }
        }
        Log.d(TAG, "After change: ");
        printList(list);
    }

    private void setPossibilityConstant(double[] list, int index, double value) {
        if (value < 0.0 || value > 1.0) {
            Log.e(TAG, "Set possibility of " + mPossibilityEntry[index] + " to " + value + " is not valid.");
        } else {
            double last_possibility = 1 - value;
            for(int i = 0; i < list.length; i++) {
                if (i == index) {
                    list[i] = value;
                } else {
                    list[i] = last_possibility / (list.length - 1);
                }
            }
        }
        Log.d(TAG, "After change: ");
        printList(list);
    }

    private void printList(double[] list) {
        for (int i = 0; i < list.length; i++)
            Log.d(TAG, "  [" + i + "] = " + list[i]);
    }
}
