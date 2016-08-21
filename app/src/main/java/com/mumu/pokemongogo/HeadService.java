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

import com.mumu.pokemongogo.location.FakeLocation;
import com.mumu.pokemongogo.location.FakeLocationManager;

public class HeadService extends Service {
    private static final String TAG = "PokemonGoGo";
    private Context mContext;
    private FakeLocationManager mFakeLocationManager;

    private WindowManager mWindowManager;
    WindowManager.LayoutParams mHeadIconLayoutParams;
    WindowManager.LayoutParams mMsgTextLayoutParams;
    WindowManager.LayoutParams mHeadStartLayoutParams;
    WindowManager.LayoutParams mHeadHomeLayoutParams;
    WindowManager.LayoutParams mHeadIncubateLayoutParams;

    // icon shown on screen
    private ImageView mHeadIconView;
    private ImageView mHeadStartView;
    private ImageView mHeadHomeView;
    private ImageView mHeadIncubateView;
    private TextView  mHeadMsgTextView;

    private String mMessageText = "Now stopping";
    private boolean mThreadStart = false;
    private GetMessageThread mMessageThread;

    private final int mInitialPositionX = 0;
    private final int mInitialPositionY = 150;
    private final int mTextOffsetX = 120;
    private final int mTextOffsetY = 13;
    private final int mToolOffsetX = 0;
    private final int mToolOffsetY = 75;
    private final int mGameOffsetX = 80;
    private final int mGameOffsetY = 85;
    private final int mIncubateOffsetX = 150;
    private final int mIncubateOffsetY = 85;

    private final Handler mHandler = new Handler();
    private final long mTouchTapThreshold = 200;  //Workaround for touch too sensitive
    private final long mTouchLongPressThreshold = 1500;

    // game control
    private boolean mFreeWalking = false;
    private boolean mAutoIncubating = false;
    private Button mUpButton, mDownButton, mLeftButton, mRightButton;
    WindowManager.LayoutParams mUpButtonLayoutParams, mDownButtonLayoutParams;
    WindowManager.LayoutParams mLeftButtonLayoutParams, mRightButtonLayoutParams;
    private StartAutoIncubatingThread mAIThread;

    /*
     * Runnable threads
     */
    private final Runnable updateRunnable = new Runnable() {
        public void run() {
            updateUI();
        }
    };

    private void updateUI() {
        mHeadMsgTextView.setText(mMessageText);
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
        initGamePanelTouchListeners();

        mThreadStart = true;
        mMessageThread = new GetMessageThread();
        mMessageThread.start();

        mAIThread = new StartAutoIncubatingThread();

        // Config fake location manager
        mFakeLocationManager = new FakeLocationManager(mContext, null);

        // Set enable
        mFakeLocationManager.setEnable(true);
    }

    private void initGamePanelViews() {
        mHeadMsgTextView = new TextView(this);
        mHeadMsgTextView.setText("");
        mHeadMsgTextView.setTextColor(Color.BLACK);
        mHeadMsgTextView.setBackgroundColor(Color.WHITE);

        mHeadIconView = new ImageView(this);
        mHeadIconView.setImageResource(R.mipmap.ic_launcher);
        mHeadStartView = new ImageView(this);
        mHeadStartView.setImageResource(R.drawable.ic_play);
        mHeadHomeView = new ImageView(this);
        mHeadHomeView.setImageResource(R.drawable.ic_location_pin);
        mHeadIncubateView = new ImageView(this);
        mHeadIncubateView.setImageResource(R.drawable.ic_egg);

        mHeadIconLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mMsgTextLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mHeadStartLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mHeadHomeLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mHeadIncubateLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mHeadIconLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mHeadIconLayoutParams.x = mInitialPositionX;
        mHeadIconLayoutParams.y = mInitialPositionY;
        mMsgTextLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mMsgTextLayoutParams.x = mInitialPositionX + mTextOffsetX;
        mMsgTextLayoutParams.y = mInitialPositionY + mTextOffsetY;
        mHeadStartLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mHeadStartLayoutParams.x = mInitialPositionX + mToolOffsetX;
        mHeadStartLayoutParams.y = mInitialPositionY + mToolOffsetY;
        mHeadHomeLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mHeadHomeLayoutParams.x = mInitialPositionX + mGameOffsetX;
        mHeadHomeLayoutParams.y = mInitialPositionY + mGameOffsetY;
        mHeadIncubateLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mHeadIncubateLayoutParams.x = mInitialPositionX + mIncubateOffsetX;
        mHeadIncubateLayoutParams.y = mInitialPositionY + mIncubateOffsetY;

        mWindowManager.addView(mHeadIconView, mHeadIconLayoutParams);
        mWindowManager.addView(mHeadMsgTextView, mMsgTextLayoutParams);
        mWindowManager.addView(mHeadStartView, mHeadStartLayoutParams);
        mWindowManager.addView(mHeadHomeView, mHeadHomeLayoutParams);
        mWindowManager.addView(mHeadIncubateView, mHeadIncubateLayoutParams);

        mHeadStartView.setVisibility(View.INVISIBLE);
        mHeadHomeView.setVisibility(View.INVISIBLE);
        mHeadIncubateView.setVisibility(View.INVISIBLE);
    }

    private void initGameControlButtons() {
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
                if (view.equals(mUpButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.NORTH);
                } else if (view.equals(mDownButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.SOUTH);
                } else if (view.equals(mLeftButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.EAST);
                } else if (view.equals(mRightButton)) {
                    mFakeLocationManager.walkPace(FakeLocation.WEST);
                }
            }
        };

        mUpButton.setOnClickListener(mWalkButtonController);
        mDownButton.setOnClickListener(mWalkButtonController);
        mLeftButton.setOnClickListener(mWalkButtonController);
        mRightButton.setOnClickListener(mWalkButtonController);
    }

    private void initGamePanelTouchListeners() {
        mHeadIconView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchDownTime;
            private long touchUpTime;
            private int touchCount = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        initialX = mHeadIconLayoutParams.x;
                        initialY = mHeadIconLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
                            touchCount++;
                            if (touchCount % 2 == 0) {
                                mHeadStartView.setVisibility(View.INVISIBLE);
                                mHeadHomeView.setVisibility(View.INVISIBLE);
                                mHeadIncubateView.setVisibility(View.INVISIBLE);
                            } else {
                                mHeadStartView.setVisibility(View.VISIBLE);
                                mHeadHomeView.setVisibility(View.VISIBLE);
                                mHeadIncubateView.setVisibility(View.VISIBLE);
                            }
                        } else if (elapsedTime > mTouchLongPressThreshold) {
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
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        MoveIcons(initialX, initialY, initialTouchX, initialTouchY, event);
                        return true;
                }
                return false;
            }
        });

        mHeadStartView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchDownTime;
            private long touchUpTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        initialX = mHeadIconLayoutParams.x;
                        initialY = mHeadIconLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
                            mFreeWalking = !mFreeWalking;
                            mMessageText = mFreeWalking ? "Starting service" : "Stopping service";
                            configFreeWalking(mFreeWalking);
                            mHeadStartView.setImageResource(mFreeWalking ? R.drawable.ic_pause : R.drawable.ic_play);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        MoveIcons(initialX, initialY, initialTouchX, initialTouchY, event);
                        return true;
                }
                return false;
            }
        });

        mHeadHomeView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchDownTime;
            private long touchUpTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        initialX = mHeadIconLayoutParams.x;
                        initialY = mHeadIconLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
                            Log.d(TAG, "Game stopped.");
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        MoveIcons(initialX, initialY, initialTouchX, initialTouchY, event);
                        return true;
                }
                return false;
            }
        });

        mHeadIncubateView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchDownTime;
            private long touchUpTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        initialX = mHeadIconLayoutParams.x;
                        initialY = mHeadIconLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
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
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        MoveIcons(initialX, initialY, initialTouchX, initialTouchY, event);
                        return true;
                }
                return false;
            }
        });
    }

    private void MoveIcons(int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event) {
        mHeadIconLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
        mHeadIconLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
        mMsgTextLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mTextOffsetX;
        mMsgTextLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mTextOffsetY;
        mHeadStartLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mToolOffsetX;
        mHeadStartLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mToolOffsetY;
        mHeadHomeLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mGameOffsetX;
        mHeadHomeLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mGameOffsetY;
        mHeadIncubateLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mIncubateOffsetX;
        mHeadIncubateLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mIncubateOffsetY;
        mWindowManager.updateViewLayout(mHeadIconView, mHeadIconLayoutParams);
        mWindowManager.updateViewLayout(mHeadMsgTextView, mMsgTextLayoutParams);
        mWindowManager.updateViewLayout(mHeadStartView, mHeadStartLayoutParams);
        mWindowManager.updateViewLayout(mHeadHomeView, mHeadHomeLayoutParams);
        mWindowManager.updateViewLayout(mHeadIncubateView, mHeadIncubateLayoutParams);
    }

    private void StopService() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHeadIconView != null) mWindowManager.removeView(mHeadIconView);
        if (mHeadMsgTextView != null) mWindowManager.removeView(mHeadMsgTextView);
        if (mHeadStartView != null) mWindowManager.removeView(mHeadStartView);
        if (mHeadHomeView != null) mWindowManager.removeView(mHeadHomeView);
        if (mUpButton != null) mWindowManager.removeView(mUpButton);
        if (mDownButton != null) mWindowManager.removeView(mDownButton);
        if (mLeftButton != null) mWindowManager.removeView(mLeftButton);
        if (mRightButton != null) mWindowManager.removeView(mRightButton);
        if (mMessageThread.isAlive()) mThreadStart = false;
        if (mFakeLocationManager != null) mFakeLocationManager.setEnable(false);
    }

    private void configFreeWalking(boolean on) {
        if (on) {
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
        if (!mAIThread.isAlive())
            mAIThread.start();
    }

    class StartAutoIncubatingThread extends Thread {
        public void run() {
            while (mAutoIncubating) {
                int direction = (int)(Math.random() * 10) % 5;
                mFakeLocationManager.walkPace(direction);
                try {
                    Thread.sleep((int)(Math.random() * 1000) + 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
