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

package com.mumu.pokemongogo;

import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mumu.pokemongogo.headicon.HeadIconView;
import com.mumu.pokemongogo.location.FakeLocation;
import com.mumu.pokemongogo.location.FakeLocationManager;
import com.mumu.pokemongogo.location.HumanWalkSimulator;

import java.util.ArrayList;

public class HeadService extends Service {
    private static final String TAG = "PokemonGoGo";
    private Context mContext;
    private FakeLocationManager mFakeLocationManager;

    private WindowManager mWindowManager;
    ArrayList<HeadIconView> mHeadIconList;
    private static final int IDX_HEAD_ICON = 0;
    private static final int IDX_MSG_TEXT = 1;
    private static final int IDX_START_ICON = 2;
    private static final int IDX_HOME_ICON = 3;
    private static final int IDX_INCUBATOR_ICON = 4;

    private String mMessageText = "Now stopping";
    private boolean mThreadStart = false;
    private GetMessageThread mMessageThread;

    private final Handler mHandler = new Handler();
    private int mTouchHeadIconCount = 0;

    // game control
    private boolean mFreeWalking = false;
    private boolean mAutoIncubating = false;
    private Button mUpButton, mDownButton, mLeftButton, mRightButton;
    WindowManager.LayoutParams mUpButtonLayoutParams, mDownButtonLayoutParams;
    WindowManager.LayoutParams mLeftButtonLayoutParams, mRightButtonLayoutParams;
    private StartAutoIncubatingThread mAIThread;
    private double mWalkSpeed = 1.0;

    /*
     * Runnable threads
     */
    private final Runnable updateRunnable = new Runnable() {
        public void run() {
            updateUI();
        }
    };

    private void updateUI() {
        ((TextView) mHeadIconList.get(IDX_MSG_TEXT).getView()).setText(mMessageText);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // On screen view initializing, do not mess around here
        initGameControlButtons();
        initGamePanelViews();

        mThreadStart = true;
        mMessageThread = new GetMessageThread();
        mMessageThread.start();

        // Config fake location manager
        mFakeLocationManager = new FakeLocationManager(mContext, null);

        // Set enable
        mFakeLocationManager.setEnable(true);
    }

    private void initGamePanelViews() {
        mHeadIconList = new ArrayList<>();

        // Head Icon
        HeadIconView headIcon = new HeadIconView(new ImageView(this), mWindowManager, 0, 0);
        ((ImageView) headIcon.getView()).setImageResource(R.mipmap.ic_launcher);
        headIcon.setOnTapListener(new HeadIconView.OnTapListener() {
            @Override
            public void onTap(View view) {
                configHeadIconShowing();
            }

            @Override
            public void onLongPress(View view) {
                showExitConfirmDialog();
            }
        });
        mHeadIconList.add(headIcon);

        // Message Text Icon
        HeadIconView msgText = new HeadIconView(new TextView(this), mWindowManager, 120, 13);
        ((TextView) msgText.getView()).setText("");
        ((TextView) msgText.getView()).setTextColor(Color.BLACK);
        msgText.getView().setBackgroundColor(Color.WHITE);
        mHeadIconList.add(msgText);

        // Start Game Pad Icon
        HeadIconView startIcon = new HeadIconView(new ImageView(this), mWindowManager, 0, 75);
        ((ImageView) startIcon.getView()).setImageResource(R.drawable.ic_play);
        startIcon.setOnTapListener(new HeadIconView.OnTapListener() {
            @Override
            public void onTap(View view) {
                configFreeWalking();
            }

            @Override
            public void onLongPress(View view) {

            }
        });
        mHeadIconList.add(startIcon);

        // Home Icon
        HeadIconView homeIcon = new HeadIconView(new ImageView(this), mWindowManager, 80, 85);
        ((ImageView) homeIcon.getView()).setImageResource(R.drawable.ic_location_pin);
        homeIcon.setOnTapListener(new HeadIconView.OnTapListener() {
            @Override
            public void onTap(View view) {
                Log.d(TAG, "config home icon");
            }

            @Override
            public void onLongPress(View view) {

            }
        });
        mHeadIconList.add(homeIcon);

        // Incubator Icon
        HeadIconView incubatorIcon = new HeadIconView(new ImageView(this), mWindowManager, 150, 85);
        ((ImageView) incubatorIcon.getView()).setImageResource(R.drawable.ic_egg);
        incubatorIcon.setOnTapListener(new HeadIconView.OnTapListener() {
            @Override
            public void onTap(View view) {
                Log.d(TAG, "config auto incubating");
                configAutoIncubating();
            }

            @Override
            public void onLongPress(View view) {

            }
        });
        mHeadIconList.add(incubatorIcon);

        // Share the same on move listener for moving in the same time
        HeadIconView.OnMoveListener moveListener = new HeadIconView.OnMoveListener() {
            @Override
            public void onMove(int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event) {
                for(HeadIconView icon : mHeadIconList) {
                    icon.moveIconDefault(initialX, initialY, initialTouchX, initialTouchY, event);
                }
            }
        };

        // Set all to add
        for(HeadIconView icon : mHeadIconList) {
            icon.addView();
            icon.setOnMoveListener(moveListener);
        }

        // Set default visibility
        mHeadIconList.get(IDX_START_ICON).setVisibility(View.INVISIBLE);
        mHeadIconList.get(IDX_HOME_ICON).setVisibility(View.INVISIBLE);
        mHeadIconList.get(IDX_INCUBATOR_ICON).setVisibility(View.INVISIBLE);
    }

    private void initGameControlButtons() {
        final int mInitialPositionX = 0;
        final int mInitialPositionY = 150;

        mUpButton = new Button(this);
        mUpButton.setText(getString(R.string.walk_up_button));
        mUpButton.setWidth(30);
        mUpButton.setHeight(30);
        mUpButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.button_half_transparent));
        mDownButton = new Button(this);
        mDownButton.setText(getString(R.string.walk_down_button));
        mDownButton.setWidth(30);
        mDownButton.setHeight(30);
        mDownButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.button_half_transparent));
        mLeftButton = new Button(this);
        mLeftButton.setText(getString(R.string.walk_left_button));
        mLeftButton.setWidth(30);
        mLeftButton.setHeight(30);
        mLeftButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.button_half_transparent));
        mRightButton = new Button(this);
        mRightButton.setText(getString(R.string.walk_right_button));
        mRightButton.setWidth(30);
        mRightButton.setHeight(30);
        mRightButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.button_half_transparent));

        mUpButtonLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mDownButtonLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mLeftButtonLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mRightButtonLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mUpButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        mUpButtonLayoutParams.x = mInitialPositionX + 150;
        mUpButtonLayoutParams.y = mInitialPositionY + 200;
        mDownButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        mDownButtonLayoutParams.x = mInitialPositionX + 150;
        mDownButtonLayoutParams.y = mInitialPositionY;
        mLeftButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        mLeftButtonLayoutParams.x = mInitialPositionX + 50;
        mLeftButtonLayoutParams.y = mInitialPositionY + 100;
        mRightButtonLayoutParams.gravity = Gravity.BOTTOM | Gravity.START;
        mRightButtonLayoutParams.x = mInitialPositionX + 230;
        mRightButtonLayoutParams.y = mInitialPositionY + 100;

        mWindowManager.addView(mUpButton, mUpButtonLayoutParams);
        mWindowManager.addView(mDownButton, mDownButtonLayoutParams);
        mWindowManager.addView(mLeftButton, mLeftButtonLayoutParams);
        mWindowManager.addView(mRightButton, mRightButtonLayoutParams);

        mUpButton.setVisibility(View.INVISIBLE);
        mDownButton.setVisibility(View.INVISIBLE);
        mLeftButton.setVisibility(View.INVISIBLE);
        mRightButton.setVisibility(View.INVISIBLE);

        final View.OnClickListener mWalkButtonController = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // make sure the speed does not get override by auto incubator
                mFakeLocationManager.setSpeed(mWalkSpeed);

                if (view.equals(mUpButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.NORTH, 1.0);
                } else if (view.equals(mDownButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.SOUTH, 1.0);
                } else if (view.equals(mLeftButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.EAST, 1.0);
                } else if (view.equals(mRightButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.WEST, 1.0);
                }
            }
        };

        mUpButton.setOnClickListener(mWalkButtonController);
        mDownButton.setOnClickListener(mWalkButtonController);
        mLeftButton.setOnClickListener(mWalkButtonController);
        mRightButton.setOnClickListener(mWalkButtonController);
    }

    private void StopService() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThreadStart = false;

        // Game tool
        for (HeadIconView icon : mHeadIconList) {
            icon.removeView();
        }

        // Game control tool
        if (mUpButton != null) mWindowManager.removeView(mUpButton);
        if (mDownButton != null) mWindowManager.removeView(mDownButton);
        if (mLeftButton != null) mWindowManager.removeView(mLeftButton);
        if (mRightButton != null) mWindowManager.removeView(mRightButton);

        if (mFakeLocationManager != null) mFakeLocationManager.setEnable(false);
    }

    private void configHeadIconShowing() {
        mTouchHeadIconCount++;
        if (mTouchHeadIconCount % 2 == 0) {
            mHeadIconList.get(IDX_START_ICON).setVisibility(View.INVISIBLE);
            mHeadIconList.get(IDX_HOME_ICON).setVisibility(View.INVISIBLE);
            mHeadIconList.get(IDX_INCUBATOR_ICON).setVisibility(View.INVISIBLE);
        } else {
            mHeadIconList.get(IDX_START_ICON).setVisibility(View.VISIBLE);
            mHeadIconList.get(IDX_HOME_ICON).setVisibility(View.VISIBLE);
            mHeadIconList.get(IDX_INCUBATOR_ICON).setVisibility(View.VISIBLE);
        }
    }

    private void showExitConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.myDialog))
                .setTitle(getString(R.string.headservice_stop_title))
                .setMessage(getString(R.string.headservice_stop_info))
                .setPositiveButton(getString(R.string.headservice_stop_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Let's do some background stuff
                        StopService();
                    }
                })
                .setNegativeButton(getString(R.string.startup_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }

    private void configFreeWalking() {
        mFreeWalking = !mFreeWalking;
        mMessageText = mFreeWalking ? "Starting service" : "Stopping service";
        mHeadIconList.get(IDX_START_ICON).getImageView().setImageResource(mFreeWalking ? R.drawable.ic_pause : R.drawable.ic_play);

        if (mFreeWalking) {
            mUpButton.setVisibility(View.VISIBLE);
            mDownButton.setVisibility(View.VISIBLE);
            mLeftButton.setVisibility(View.VISIBLE);
            mRightButton.setVisibility(View.VISIBLE);
        } else {
            mUpButton.setVisibility(View.INVISIBLE);
            mDownButton.setVisibility(View.INVISIBLE);
            mLeftButton.setVisibility(View.INVISIBLE);
            mRightButton.setVisibility(View.INVISIBLE);
        }
    }

    private void configAutoIncubating() {
        if (mAutoIncubating) {
            Log.d(TAG, "Stop auto incubating");
            mMessageText = "Stop Auto Incubating";
            mAutoIncubating = false;
        } else {
            Log.d(TAG, "Start auto incubating");
            mMessageText = "Start Auto Incubating";
            mAutoIncubating = true;
            startAutoIncubating();
        }
    }

    class GetMessageThread extends Thread {
        public void run() {
            while (mThreadStart) {
                mHandler.post(updateRunnable);

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void startAutoIncubating() {
        mAIThread = new StartAutoIncubatingThread();
        mAIThread.start();
    }

    class StartAutoIncubatingThread extends Thread {
        public void run() {
            HumanWalkSimulator walkSimulator = new HumanWalkSimulator();

            while (mAutoIncubating) {
                int nextDirection = walkSimulator.getNextDirection();
                double speedChange = Math.random() + 1.0; // limit speed to 1 to 1.5
                double directionRatio = 1.0 - Math.random() / 4.0; // the ratio of the direction, limit to 0.75 ~ 1
                mFakeLocationManager.walkPace(nextDirection, directionRatio);
                mFakeLocationManager.setSpeed(speedChange);
                Log.d(TAG, "Now go " + nextDirection);
                try {
                    Thread.sleep((int)(Math.random() * 1000) + 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
