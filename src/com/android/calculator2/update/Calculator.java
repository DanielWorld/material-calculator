package com.android.calculator2.update;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calculator2.AnimatorListenerWrapper;
import com.android.calculator2.CalculatorEditText;
import com.android.calculator2.CalculatorExpressionBuilder;
import com.android.calculator2.CalculatorExpressionEvaluator;
import com.android.calculator2.CalculatorExpressionTokenizer;
import com.android.calculator2.NineOldViewPager;
import com.android.calculator2.R;
import com.android.calculator2.Utils;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;

/**
 * Copyright (C) 2014-2017 daniel@bapul.net
 * Created by Daniel on 2017-07-07.
 */

public abstract class Calculator extends Fragment
        implements CalculatorEditText.OnTextSizeChangeListener, CalculatorExpressionEvaluator.EvaluateCallback, View.OnLongClickListener, View.OnClickListener {

    private static final String NAME = Calculator.class.getName();

    // instance state keys
    private static final String KEY_CURRENT_STATE = NAME + "_currentState";
    private static final String KEY_CURRENT_EXPRESSION = NAME + "_currentExpression";

    /**
     * Constant for an invalid resource id.
     */
    public static final int INVALID_RES_ID = -1;

    protected enum CalculatorState {
        INPUT, EVALUATE, RESULT, ERROR
    }

    private final TextWatcher mFormulaTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            setState(CalculatorState.INPUT);
            mEvaluator.evaluate(editable, Calculator.this);
        }
    };

    private final View.OnKeyListener mFormulaOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                        onEquals();
                    }
                    // ignore all other actions
                    return true;
            }
            return false;
        }
    };

    private final Editable.Factory mFormulaEditableFactory = new Editable.Factory() {
        @Override
        public Editable newEditable(CharSequence source) {
            final boolean isEdited = mCurrentState == CalculatorState.INPUT
                    || mCurrentState == CalculatorState.ERROR;
            return new CalculatorExpressionBuilder(source, mTokenizer, isEdited);
        }
    };

    private CalculatorState mCurrentState;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;

    protected TouchInterceptorLinearLayout mPadContainer;
    protected RelativeLayout mDisplayView;
    protected CalculatorEditText mFormulaEditText;
    protected CalculatorEditText mResultEditText;
    private NineOldViewPager mPadViewPager;
    private View mDeleteButton;
    private View mClearButton;
    private View mEqualButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calculator, container, false);

        mPadContainer = (TouchInterceptorLinearLayout) v.findViewById(R.id.pad_container);
        mDisplayView = (RelativeLayout) v.findViewById(R.id.display);
        mFormulaEditText = (CalculatorEditText) v.findViewById(R.id.formula);
        mResultEditText = (CalculatorEditText) v.findViewById(R.id.result);
        mPadViewPager = (NineOldViewPager) v.findViewById(R.id.pad_pager);
        mDeleteButton = v.findViewById(R.id.del);
        mClearButton = v.findViewById(R.id.clr);

        mEqualButton = v.findViewById(R.id.pad_numeric).findViewById(R.id.eq);
        if (mEqualButton == null || mEqualButton.getVisibility() != View.VISIBLE) {
            mEqualButton = v.findViewById(R.id.pad_operator).findViewById(R.id.eq);
        }

        v.findViewById(R.id.fun_cos).setOnClickListener(this);
        v.findViewById(R.id.fun_ln).setOnClickListener(this);
        v.findViewById(R.id.fun_log).setOnClickListener(this);
        v.findViewById(R.id.fun_sin).setOnClickListener(this);
        v.findViewById(R.id.fun_tan).setOnClickListener(this);

        v.findViewById(R.id.op_fact).setOnClickListener(this);
        v.findViewById(R.id.const_pi).setOnClickListener(this);
        v.findViewById(R.id.const_e).setOnClickListener(this);
        v.findViewById(R.id.lparen).setOnClickListener(this);
        v.findViewById(R.id.rparen).setOnClickListener(this);

        v.findViewById(R.id.op_sqrt).setOnClickListener(this);
        v.findViewById(R.id.op_pow).setOnClickListener(this);
        v.findViewById(R.id.op_div).setOnClickListener(this);
        v.findViewById(R.id.op_mul).setOnClickListener(this);
        v.findViewById(R.id.op_sub).setOnClickListener(this);
        v.findViewById(R.id.op_add).setOnClickListener(this);

        v.findViewById(R.id.digit_7).setOnClickListener(this);
        v.findViewById(R.id.digit_8).setOnClickListener(this);
        v.findViewById(R.id.digit_9).setOnClickListener(this);
        v.findViewById(R.id.digit_4).setOnClickListener(this);
        v.findViewById(R.id.digit_5).setOnClickListener(this);
        v.findViewById(R.id.digit_6).setOnClickListener(this);
        v.findViewById(R.id.digit_1).setOnClickListener(this);
        v.findViewById(R.id.digit_2).setOnClickListener(this);
        v.findViewById(R.id.digit_3).setOnClickListener(this);
        v.findViewById(R.id.digit_0).setOnClickListener(this);

        v.findViewById(R.id.dec_point).setOnClickListener(this);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTokenizer = new CalculatorExpressionTokenizer(getContext());
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        savedInstanceState = savedInstanceState == null ? Bundle.EMPTY : savedInstanceState;
        setState(CalculatorState.values()[
                savedInstanceState.getInt(KEY_CURRENT_STATE, CalculatorState.INPUT.ordinal())]);
        String keyCurrentExpr = savedInstanceState.getString(KEY_CURRENT_EXPRESSION);
        mFormulaEditText.setText(mTokenizer.getLocalizedExpression(
                keyCurrentExpr == null ? "" : keyCurrentExpr));
        mEvaluator.evaluate(mFormulaEditText.getText(), this);

        mFormulaEditText.setEditableFactory(mFormulaEditableFactory);
        mFormulaEditText.addTextChangedListener(mFormulaTextWatcher);
        mFormulaEditText.setOnKeyListener(mFormulaOnKeyListener);
        mFormulaEditText.setOnTextSizeChangeListener(this);
        mDeleteButton.setOnLongClickListener(this);

        mEqualButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mClearButton.setOnClickListener(this);

        mPadContainer.setOnTouchPointerListener(mListener);

        mPadViewPager.setOnPageChangeListener(new NineOldViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d("BAD", "Page Position : " + position);

                if (position == 1) {
                    if (mPadContainer != null)
                        mPadContainer.setPadCosXY(130, 600);
                } else {
                    if (mPadContainer != null)
                        mPadContainer.setPadCosXY(700, 600);
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private TouchInterceptorLinearLayout.OnTouchPointerListener mListener;
    public void setOnTouchPointerListener(TouchInterceptorLinearLayout.OnTouchPointerListener listener) {
        mListener = listener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // If there's an animation in progress, end it immediately to ensure the state is
        // up-to-date before it is serialized.
        cancelAnimation();

        super.onSaveInstanceState(outState);

        outState.putInt(KEY_CURRENT_STATE, mCurrentState.ordinal());
        outState.putString(KEY_CURRENT_EXPRESSION,
                mTokenizer.getNormalizedExpression(mFormulaEditText.getText().toString()));
    }

    protected void setState(CalculatorState state) {
        if (mCurrentState != state) {
            mCurrentState = state;

            if (state == CalculatorState.RESULT || state == CalculatorState.ERROR) {
                mDeleteButton.setVisibility(View.GONE);
                mClearButton.setVisibility(View.VISIBLE);
            } else {
                mDeleteButton.setVisibility(View.VISIBLE);
                mClearButton.setVisibility(View.GONE);
            }

            if (state == CalculatorState.ERROR) {
                final int errorColor = getResources().getColor(R.color.calculator_error_color);
                mFormulaEditText.setTextColor(errorColor);
                mResultEditText.setTextColor(errorColor);
                Utils.setStatusBarColorCompat(getActivity().getWindow(), errorColor);
            } else {
                mFormulaEditText.setTextColor(
                        getResources().getColor(R.color.display_formula_text_color));
                mResultEditText.setTextColor(
                        getResources().getColor(R.color.display_result_text_color));
                Utils.setStatusBarColorCompat(getActivity().getWindow(), getResources().getColor(R.color.calculator_accent_color));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.eq:
                onEquals();
                break;
            case R.id.del:
                onDelete();
                break;
            case R.id.clr:
                onClear();
                break;
            case R.id.fun_cos:
            case R.id.fun_ln:
            case R.id.fun_log:
            case R.id.fun_sin:
            case R.id.fun_tan:
                // Add left parenthesis after functions.
                mFormulaEditText.append(((Button) v).getText() + "(");
                break;
            default:
                mFormulaEditText.append(((Button) v).getText());
                break;
        }
    }


    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.del) {
            onClear();
            return true;
        }
        return false;
    }

    @Override
    public void onEvaluate(String expr, String result, int errorResourceId) {
        if (mCurrentState == CalculatorState.INPUT) {
            mResultEditText.setText(result);
        } else if (errorResourceId != INVALID_RES_ID) {
            onError(errorResourceId);
        } else if (!TextUtils.isEmpty(result)) {
            onResult(result);
        } else if (mCurrentState == CalculatorState.EVALUATE) {
            // The current expression cannot be evaluated -> return to the input state.
            setState(CalculatorState.INPUT);
        }

        mFormulaEditText.requestFocus();
    }

    @Override
    public void onTextSizeChanged(TextView textView, float oldSize) {
        if (mCurrentState != CalculatorState.INPUT) {
            // Only animate text changes that occur from user input.
            return;
        }

        // Calculate the values needed to perform the scale and translation animations,
        // maintaining the same apparent baseline for the displayed text.
        final float textScale = oldSize / textView.getTextSize();
        final float translationX = (1.0f - textScale) *
                (textView.getWidth() / 2.0f - ViewCompat.getPaddingEnd(textView));
        final float translationY = (1.0f - textScale) *
                (textView.getHeight() / 2.0f - textView.getPaddingBottom());

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(textView, "scaleX", textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, "scaleY", textScale, 1.0f),
                ObjectAnimator.ofFloat(textView, "translationX", translationX, 0.0f),
                ObjectAnimator.ofFloat(textView, "translationY", translationY, 0.0f));
        animatorSet.setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime));
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void onEquals() {
        if (mCurrentState == CalculatorState.INPUT) {
            setState(CalculatorState.EVALUATE);
            mEvaluator.evaluate(mFormulaEditText.getText(), this);
        }
    }

    private void onDelete() {
        // Delete works like backspace; remove the last character from the expression.
        final Editable formulaText = mFormulaEditText.getEditableText();
        final int formulaLength = formulaText.length();
        if (formulaLength > 0) {
            formulaText.delete(formulaLength - 1, formulaLength);
        }
    }

    abstract void cancelAnimation();

    abstract void reveal(View sourceView, int colorRes, AnimatorListenerWrapper listener);

    private void onClear() {
        if (TextUtils.isEmpty(mFormulaEditText.getText())) {
            return;
        }

        final View sourceView = mClearButton.getVisibility() == View.VISIBLE
                ? mClearButton : mDeleteButton;
        reveal(sourceView, R.color.calculator_accent_color, new AnimatorListenerWrapper() {
            @Override
            public void onAnimationStart() {
                mFormulaEditText.getEditableText().clear();
            }
        });
    }

    private void onError(final int errorResourceId) {
        if (mCurrentState != CalculatorState.EVALUATE) {
            // Only animate error on evaluate.
            mResultEditText.setText(errorResourceId);
            return;
        }

        reveal(mEqualButton, R.color.calculator_error_color, new AnimatorListenerWrapper() {
            @Override
            public void onAnimationStart() {
                setState(CalculatorState.ERROR);
                mResultEditText.setText(errorResourceId);
            }
        });
    }

    abstract void onResult(final String result);
}
