/*
 Copyright 2011, 2012 Chris Banes.
 <p/>
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 <p/>
 http://www.apache.org/licenses/LICENSE-2.0
 <p/>
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.github.chrisbanes.photoview;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

/**
 * Does a whole lot of gesture detecting.
 */
class CustomGestureDetector {

    private static final int INVALID_POINTER_ID = -1;

    private int mActivePointerId = INVALID_POINTER_ID;
    private int mActivePointerIndex = 0;
    private final ScaleGestureDetector mDetector;

    private VelocityTracker mVelocityTracker;
    private boolean mIsDragging;
    private float mLastTouchX;
    private float mLastTouchY;
    private final float mTouchSlop;
    private final float mMinimumVelocity;
    private OnGestureListener mListener;

    CustomGestureDetector(Context context, OnGestureListener listener) {
        final ViewConfiguration configuration = ViewConfiguration
                .get(context);
        //获得允许执行一个fling手势动作的最小速度值
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();

        mListener = listener;
        ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();

                if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
                    return false;
             
                if (scaleFactor >= 0) {
                    mListener.onScale(scaleFactor, detector.getFocusX(), detector.getFocusY());
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // NO-OP
            }
        };
        mDetector = new ScaleGestureDetector(context, mScaleListener);
    }

    private float getActiveX(MotionEvent ev) {
        try {
            return ev.getX(mActivePointerIndex);
        } catch (Exception e) {
            return ev.getX();
        }
    }

    private float getActiveY(MotionEvent ev) {
        try {
            return ev.getY(mActivePointerIndex);
        } catch (Exception e) {
            return ev.getY();
        }
    }

    public boolean isScaling() {
        return mDetector.isInProgress();
    }

    public boolean isDragging() {
        return mIsDragging;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        try {
            mDetector.onTouchEvent(ev);
            return processTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            // Fix for support lib bug, happening when onDestroy is called
            return true;
        }
    }
    //TODO 1.演示：速率的使用  用来判断是fling
    //TODO 1.演示：速率的使用  用来判断是fling


    private boolean processTouchEvent(MotionEvent ev) {
        int multiAction=ev.getActionMasked();

        //final int action = ev.getAction();
        final int action=ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
               //按下的时候获取速度追踪器
                mVelocityTracker = VelocityTracker.obtain();
                if (null != mVelocityTracker) {
                    mVelocityTracker.addMovement(ev);
                }
                //当前index的x值
                mLastTouchX = getActiveX(ev);
                mLastTouchY = getActiveY(ev);
                mIsDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float x = getActiveX(ev);
                final float y = getActiveY(ev);
                final float dx = x - mLastTouchX, dy = y - mLastTouchY;

                if (!mIsDragging) {
                    // Use Pythagoras to see if drag length is larger than
                    // touch slop
                    //最小滑动距离 使用勾股定理
                    mIsDragging = Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
                }

                if (mIsDragging) {
                    //超过最小滑动距离，就算是拖拽
                    mListener.onDrag(dx, dy);
                    mLastTouchX = x;
                    mLastTouchY = y;

                    if (null != mVelocityTracker) {
                        mVelocityTracker.addMovement(ev);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = INVALID_POINTER_ID;
                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_UP:
                mActivePointerId = INVALID_POINTER_ID;
                if (mIsDragging) {
                    if (null != mVelocityTracker) {
                        mLastTouchX = getActiveX(ev);
                        mLastTouchY = getActiveY(ev);

                        // Compute velocity within the last 1000ms
                        mVelocityTracker.addMovement(ev);
                        mVelocityTracker.computeCurrentVelocity(1000);

                        final float vX = mVelocityTracker.getXVelocity(), vY = mVelocityTracker
                                .getYVelocity();

                        // If the velocity is greater than minVelocity, call
                        // listener
                        //比最小的速度还大，叫表示fling
                        if (Math.max(Math.abs(vX), Math.abs(vY)) >= mMinimumVelocity) {
                            mListener.onFling(mLastTouchX, mLastTouchY, -vX,
                                    -vY);
                        }
                    }
                }

                // Recycle Velocity Tracker
                if (null != mVelocityTracker) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //final int pointerIndex = Util.getPointerIndex(ev.getAction());
                final int pointerIndex = ev.getActionIndex();
                System.out.println("pointerIndex-----up-->"+pointerIndex+"------");
                //拿到真是的pointerId
                final int pointerId = ev.getPointerId(pointerIndex);

                //如果 我的第1根手指（pointerId）==0；
                System.out.println(" mActivePointerId--》"+ mActivePointerId+"-------pointerId----》"+pointerId);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    //如果此时pointerId为0的pointerIndex不一定为0，
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                }
                break;
        }

        mActivePointerIndex = ev.findPointerIndex(mActivePointerId != INVALID_POINTER_ID ? mActivePointerId : 0);
        return true;
    }
}
