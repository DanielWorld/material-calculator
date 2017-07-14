package com.android.calculator2;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.android.calculator2.update.CalculatorGBFragment;
import com.android.calculator2.update.CalculatorLFragment;
import com.android.calculator2.update.TouchInterceptorLinearLayout;

public class StartActivity extends FragmentActivity implements TouchInterceptorLinearLayout.OnTouchPointerListener{

    private RelativeLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        contentFrame = (RelativeLayout) findViewById(R.id.contentFrame);

        findViewById(R.id.showCal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentFrame.setVisibility(contentFrame.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        if (Utils.hasLollipop()) {

            CalculatorLFragment calculatorLFragment
                    = (CalculatorLFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);
            if (calculatorLFragment == null) {
                calculatorLFragment = CalculatorLFragment.newInstance();
                calculatorLFragment.setOnTouchPointerListener(this);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.contentFrame, calculatorLFragment);
                transaction.commit();
            }
        } else {
            CalculatorGBFragment calculatorLFragment
                    = (CalculatorGBFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);
            if (calculatorLFragment == null) {
                calculatorLFragment = CalculatorGBFragment.newInstance();
                calculatorLFragment.setOnTouchPointerListener(this);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.contentFrame, calculatorLFragment);
                transaction.commit();
            }
        }
    }

    private float initialTouchX, initialTouchY;
    private float moveTouchX, moveTouchY;
    private float lastTouchX, lastTouchY;
    // 기준점 x, y
    private float standardX = 0;
    private float standardY = 0;

    @Override
    public void onTouchDown(float x, float y) {
        Log.e("OKAY", "Down : " + x + " : " + y);
        initialTouchX = x;
        initialTouchY = y;
    }

    @Override
    public void onTouchMove(float x, float y) {
        moveTouchX = x - initialTouchX;
        moveTouchY = y - initialTouchY;
        Log.d("OKAY", "Move : " + moveTouchX + " : " + moveTouchY);

        animatePopupView(contentFrame, (int) (moveTouchX + standardX), (int) (moveTouchY + standardY));
//         TODO: 애니메이션의 경우 시작시 이전 애니메이션 기록과 상관없이 무조건 0, 0 을 기준으로 이동한다!
//        animatePopupView(contentFrame, 50, 50);
    }

    @Override
    public void onTouchUp(float x, float y) {
        lastTouchX = x;
        lastTouchY = y;
        Log.i("OKAY", "Up : " + lastTouchX + " : " + lastTouchY);

        standardX += (lastTouchX - initialTouchX);
        standardY += (lastTouchY - initialTouchY);
    }

    public void animatePopupView(View v, float x, float y) {
        AnimatorSet setAnimation = new AnimatorSet();
        setAnimation.play(translationX(v, x))
                .with(translationY(v, y));
        setAnimation.start();
    }

    public ObjectAnimator translationX(View v, float value) {
        ObjectAnimator translationAnimation = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, value);
        translationAnimation.setDuration(0);
        return translationAnimation;
    }

    public ObjectAnimator translationY(View v, float value) {
        ObjectAnimator translationAnimation = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, value);
        translationAnimation.setDuration(0);
        return translationAnimation;
    }

    @Override
    public void onBackPressed() {
        // TODO: 나중에 애니메이션으로 가려주면 더 좋을 듯 하다!
        if (contentFrame.getVisibility() == View.VISIBLE) {
            contentFrame.setVisibility(View.GONE);
            return;
        }

        super.onBackPressed();
    }
}
