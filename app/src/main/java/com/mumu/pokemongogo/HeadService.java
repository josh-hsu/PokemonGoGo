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
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class HeadService extends Service {
    private static final String TAG = "PokemonGoGo";
    private Context mContext;

    private WindowManager mWindowManager;
    WindowManager.LayoutParams mHeadIconLayoutParams;
    WindowManager.LayoutParams mMsgTextLayoutParams;
    WindowManager.LayoutParams mHeadStartLayoutParams;
    WindowManager.LayoutParams mHeadHomeLayoutParams;

    // icon shown on screen
    private ImageView mHeadIconView;
    private ImageView mHeadStartView;
    private ImageView mHeadHomeView;
    private TextView  mHeadMsgTextView;

    private String mMessageText = "Now stopping";
    private boolean mThreadStart = false;
    private GetMessageThread mMessageThread;

    final int mInitialPositionX = 0;
    final int mInitialPositionY = 150;
    final int mTextOffsetX = 120;
    final int mTextOffsetY = 13;
    final int mToolOffsetX = 0;
    final int mToolOffsetY = 75;
    final int mGameOffsetX = 70;
    final int mGameOffsetY = 75;

    private final Handler mHandler = new Handler();
    private final long mTouchTapThreshold = 200;  //Workaround for touch too sensitive
    private final long mTouchLongPressThreshold = 1500;

    final Runnable updateRunnable = new Runnable() {
        public void run() {
            updateUI();
        }
    };
    private final Runnable mDumpScreenRunnable = new Runnable() {
        @Override
        public void run() {
            /* show icon view back */
            configIconViews(true);
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

        mWindowManager.addView(mHeadIconView, mHeadIconLayoutParams);
        mWindowManager.addView(mHeadMsgTextView, mMsgTextLayoutParams);
        mWindowManager.addView(mHeadStartView, mHeadStartLayoutParams);
        mWindowManager.addView(mHeadHomeView, mHeadHomeLayoutParams);
        mHeadStartView.setVisibility(View.INVISIBLE);
        mHeadHomeView.setVisibility(View.INVISIBLE);

        mThreadStart = true;
        mMessageThread = new GetMessageThread();
        mMessageThread.start();

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
                            } else {
                                mHeadStartView.setVisibility(View.VISIBLE);
                                mHeadHomeView.setVisibility(View.VISIBLE);
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
                            configIconViews(false);
                            mHandler.postDelayed(mDumpScreenRunnable, 100);
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
        mWindowManager.updateViewLayout(mHeadIconView, mHeadIconLayoutParams);
        mWindowManager.updateViewLayout(mHeadMsgTextView, mMsgTextLayoutParams);
        mWindowManager.updateViewLayout(mHeadStartView, mHeadStartLayoutParams);
        mWindowManager.updateViewLayout(mHeadHomeView, mHeadHomeLayoutParams);
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
        if (mMessageThread.isAlive())
            mThreadStart = false;
    }

    private void configIconViews(boolean show) {
        if (show) {
            mHeadStartView.setVisibility(View.VISIBLE);
            mHeadIconView.setVisibility(View.VISIBLE);
            mHeadHomeView.setVisibility(View.VISIBLE);
        } else {
            mHeadStartView.setVisibility(View.INVISIBLE);
            mHeadIconView.setVisibility(View.INVISIBLE);
            mHeadHomeView.setVisibility(View.INVISIBLE);
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
}
