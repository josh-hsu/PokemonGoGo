package com.mumu.pokemongogo.headicon;

import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class HeadIconView {
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;
    private View mView;
    private OnTapListener mOnTapListener = null;
    private OnMoveListener mOnMoveListener = null;

    private int mOffsetX, mOffsetY;
    private boolean mIsMovable = true;

    private static final int mTouchTapThreshold = 200;
    private static final long mTouchLongPressThreshold = 1500;
    private static final int mInitialPositionX = 0;
    private static final int mInitialPositionY = 150;

    public HeadIconView(View view, WindowManager wm, int offsetX, int offsetY) {
        if (view == null)
            return;

        mView = view;
        mWindowManager = wm;
        mOffsetX = offsetX;
        mOffsetY = offsetY;

        mLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mLayoutParams.x = mInitialPositionX + mOffsetX;
        mLayoutParams.y = mInitialPositionY + mOffsetY;

        mView.setOnTouchListener(new View.OnTouchListener() {
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
                        initialX = mLayoutParams.x;
                        initialY = mLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        touchUpTime = System.currentTimeMillis();
                        long elapsedTime = touchUpTime - touchDownTime;
                        if (elapsedTime < mTouchTapThreshold) {
                            if (mOnTapListener != null)
                                mOnTapListener.onTap(mView);
                        } else if (elapsedTime > mTouchLongPressThreshold) {
                            if (mOnTapListener != null)
                                mOnTapListener.onLongPress(mView);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (mIsMovable && mOnMoveListener != null) {
                            mOnMoveListener.onMove(initialX, initialY, initialTouchX, initialTouchY, event);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public void moveIconDefault(int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event) {
        mLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX) + mOffsetX;
        mLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY) + mOffsetY;
        mWindowManager.updateViewLayout(mView, mLayoutParams);
    }

    public View getView() {
        return mView;
    }

    public ImageView getImageView() {
        return (ImageView) mView;
    }

    public void addView() {
        mWindowManager.addView(mView, mLayoutParams);
    }

    public void setVisibility(int visibility) {
        mView.setVisibility(visibility);
    }

    public void removeView() {
        if (mView != null && mWindowManager != null)
            mWindowManager.removeView(mView);
    }

    public void setOnTapListener (OnTapListener o) {
        mOnTapListener = o;
    }

    public void setOnMoveListener (OnMoveListener o) {
        mOnMoveListener = o;
    }

    public interface OnTapListener {
        void onTap(View view);
        void onLongPress(View view);
    }

    public interface OnMoveListener {
        void onMove(int initialX, int initialY, float initialTouchX, float initialTouchY, MotionEvent event);
    }
}
