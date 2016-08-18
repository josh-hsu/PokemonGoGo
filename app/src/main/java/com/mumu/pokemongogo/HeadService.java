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
    private static final String TAG = "MU774";
    private WindowManager mWindowManager;
    private ImageView iconView;
    private ImageView toolView;
    private ImageView gameView;
    private TextView messageTextView;
    private String mMessageText = "";
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
    WindowManager.LayoutParams iconLayoutParams;
    WindowManager.LayoutParams textLayoutParams;
    WindowManager.LayoutParams toolLayoutParams;
    WindowManager.LayoutParams gameLayoutParams;
    private final Handler mHandler = new Handler();
    private final long mTouchTapThreshold = 200;  //Workaround for touch too sensitive
    private final long mTouchLongPressThreshold = 1500;
    private Context mContext;

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
        messageTextView.setText(mMessageText);
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
        messageTextView = new TextView(this);
        messageTextView.setText("");
        messageTextView.setTextColor(Color.BLACK);
        messageTextView.setBackgroundColor(Color.WHITE);

        iconView = new ImageView(this);
        iconView.setImageResource(R.mipmap.ic_launcher);
        toolView = new ImageView(this);
        toolView.setImageResource(R.mipmap.ic_launcher);
        gameView = new ImageView(this);
        gameView.setImageResource(R.mipmap.ic_launcher);

        iconLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        textLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        toolLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        gameLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        iconLayoutParams.gravity = Gravity.TOP | Gravity.START;
        iconLayoutParams.x = mInitialPositionX;
        iconLayoutParams.y = mInitialPositionY;
        textLayoutParams.gravity = Gravity.TOP | Gravity.START;
        textLayoutParams.x = mInitialPositionX + mTextOffsetX;
        textLayoutParams.y = mInitialPositionY + mTextOffsetY;
        toolLayoutParams.gravity = Gravity.TOP | Gravity.START;
        toolLayoutParams.x = mInitialPositionX + mToolOffsetX;
        toolLayoutParams.y = mInitialPositionY + mToolOffsetY;
        gameLayoutParams.gravity = Gravity.TOP | Gravity.START;
        gameLayoutParams.x = mInitialPositionX + mGameOffsetX;
        gameLayoutParams.y = mInitialPositionY + mGameOffsetY;

        mWindowManager.addView(iconView, iconLayoutParams);
        mWindowManager.addView(messageTextView, textLayoutParams);
        mWindowManager.addView(toolView, toolLayoutParams);
        mWindowManager.addView(gameView, gameLayoutParams);
        toolView.setVisibility(View.INVISIBLE);
        gameView.setVisibility(View.INVISIBLE);

        mThreadStart = true;
        mMessageThread = new GetMessageThread();
        mMessageThread.start();

        iconView.setOnTouchListener(new View.OnTouchListener() {
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
                        initialX = iconLayoutParams.x;
                        initialY = iconLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
                            touchCount++;
                            if (touchCount % 2 == 0) {
                                toolView.setVisibility(View.INVISIBLE);
                                gameView.setVisibility(View.INVISIBLE);
                            } else {
                                toolView.setVisibility(View.VISIBLE);
                                gameView.setVisibility(View.VISIBLE);
                            }
                        } else if (elapsedTime > mTouchLongPressThreshold) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mContext, R.style.myDialog))
                                    .setTitle("Title")
                                    .setMessage("Msg")
                                    .setPositiveButton("Positive", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Let's do some background stuff
                                            StopService();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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

        toolView.setOnTouchListener(new View.OnTouchListener() {
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
                        initialX = iconLayoutParams.x;
                        initialY = iconLayoutParams.y;
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

        gameView.setOnTouchListener(new View.OnTouchListener() {
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
                        initialX = iconLayoutParams.x;
                        initialY = iconLayoutParams.y;
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
        iconLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
        iconLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
        textLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mTextOffsetX;
        textLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mTextOffsetY;
        toolLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mToolOffsetX;
        toolLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mToolOffsetY;
        gameLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mGameOffsetX;
        gameLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mGameOffsetY;
        mWindowManager.updateViewLayout(iconView, iconLayoutParams);
        mWindowManager.updateViewLayout(messageTextView, textLayoutParams);
        mWindowManager.updateViewLayout(toolView, toolLayoutParams);
        mWindowManager.updateViewLayout(gameView, gameLayoutParams);
    }

    private void StopService() {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iconView != null) mWindowManager.removeView(iconView);
        if (messageTextView != null) mWindowManager.removeView(messageTextView);
        if (toolView != null) mWindowManager.removeView(toolView);
        if (gameView != null) mWindowManager.removeView(gameView);
        if (mMessageThread.isAlive())
            mThreadStart = false;
    }

    private void configIconViews(boolean show) {
        if (show) {
            toolView.setVisibility(View.VISIBLE);
            iconView.setVisibility(View.VISIBLE);
            gameView.setVisibility(View.VISIBLE);
        } else {
            toolView.setVisibility(View.INVISIBLE);
            iconView.setVisibility(View.INVISIBLE);
            gameView.setVisibility(View.INVISIBLE);
        }
    }

    class GetMessageThread extends Thread {
        public void run() {
            while (mThreadStart) {
                mMessageText = "HAHA";
                mHandler.post(updateRunnable);

                try {
                    Thread.sleep(80);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
