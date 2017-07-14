package com.android.calculator2.update;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Copyright (C) 2014-2017 daniel@bapul.net
 * Created by Daniel on 2017-07-07.
 */

public class TouchInterceptorLinearLayout extends LinearLayout {
    public TouchInterceptorLinearLayout(Context context) {
        super(context);
    }

    public TouchInterceptorLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchInterceptorLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TouchInterceptorLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private OnTouchPointerListener mListener;

    public void setOnTouchPointerListener(OnTouchPointerListener listener) {
        this.mListener = listener;
    }

    public interface OnTouchPointerListener {

        void onTouchDown(float x, float y);

        void onTouchMove(float x, float y);

        void onTouchUp(float x, float y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (mListener != null)
                    mListener.onTouchDown(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_UP:
                if (mListener != null)
                    mListener.onTouchUp(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_MOVE:
                if (mListener != null)
                    mListener.onTouchMove(event.getRawX(), event.getRawY());
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        return super.onInterceptTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onTouchEvent(ev);

                float x = ev.getX();
                float y = ev.getY();
                Log.d("GOOD", x + " : " + y);

                // TODO: 실제 view 의 위치를 dp 로 체크해야 함!!!
                // Pad cos 가 접혀져 있는 경우와 펴져 있는 경우의 크기가 다르므로 이 부분도 체크가 필요하다.
                isPadCosPointerAvailable = (x > padCosX && y > padCosY);
                initTouchTime = System.currentTimeMillis();

                return false;
            case MotionEvent.ACTION_UP:
                onTouchEvent(ev);
                // TODO: 초기화는 외부에서 Pad cos 가 열렸는지 여부에 따라서 결정하도록 해야 함
                return false;
            case MotionEvent.ACTION_MOVE:

                // MOVE 가 적용되게 하려면 down 후 0.130 이상은 지나야 한다.
                if (System.currentTimeMillis() - initTouchTime < 130)
                    return false;

                // 좌표가 Pas cos 범위 안에 없다면 pad cos 이동 시도
                if (!isPadCosPointerAvailable) {
                    onTouchEvent(ev);
                    return true;
                } else {
                    return false;
                }
        }
        return false;
    }

    private long initTouchTime = 0L;        // 터치시 시간 체크

    private boolean isPadCosPointerAvailable = true;        // 해당 좌표가 Pas cos 안에 존재하는지 체크

    // pad cos 의 x, y 좌표 (opened 와 closed 되었을 때의 크기가 다르다)
    private int padCosX = 700;
    private int padCosY = 600;

    /**
     * pad cos 의 x, y 좌표 설정
     * @param x
     * @param y
     */
    public void setPadCosXY(int x, int y) {
        this.padCosX = x;
        this.padCosY = y;
    }
}
