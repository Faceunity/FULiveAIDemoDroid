package com.faceunity.nama.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.faceunity.nama.R;

public class SwiButton extends FrameLayout implements View.OnClickListener {
    private Context mContext;
    private LinearLayout llBg;
    private TextView tvLeft;
    private TextView tvRight;
    private boolean mIsSelectLeft = true;

    private ValueAnimator mLeftToRightAnimator;

    private static final int ANIMATION_DURATION = 200;
    private ValueAnimator mRightToLeftAnimator;

    public SwiButton(@NonNull Context context) {
        this(context, null);
    }

    public SwiButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwiButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    public void init() {
        //加载布局
        View view = inflate(mContext, R.layout.layout_switch_button, this);
        llBg = view.findViewById(R.id.ll_bg);
        tvLeft = view.findViewById(R.id.tv_left);
        tvRight = view.findViewById(R.id.tv_right);

        tvLeft.setOnClickListener(this);
        tvRight.setOnClickListener(this);
    }

    private void leftToRight() {
        if (!mIsSelectLeft) {
            return;
        }
        mIsSelectLeft = false;

        if (mLeftToRightAnimator != null && mLeftToRightAnimator.isRunning()) {
            mLeftToRightAnimator.end();
        }

        mLeftToRightAnimator = ValueAnimator.ofInt(getPaddingLeft(), getWidth() / 2).setDuration(ANIMATION_DURATION);
        mLeftToRightAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            llBg.setTranslationX(value);
        });
        mLeftToRightAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //选择右边完成
                if (mOnChoiceListener != null)
                    mOnChoiceListener.onChoiceButton(ButtonEnum.RIGHT);
            }
        });
        mLeftToRightAnimator.start();
    }

    private void rightToLeft() {
        if (mIsSelectLeft) {
            return;
        }
        mIsSelectLeft = true;

        if (mRightToLeftAnimator != null && mRightToLeftAnimator.isRunning()) {
            mRightToLeftAnimator.end();
        }

        mRightToLeftAnimator = ValueAnimator.ofInt(getWidth() / 2, getPaddingLeft()).setDuration(ANIMATION_DURATION);
        mRightToLeftAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            llBg.setTranslationX(value);
        });
        mRightToLeftAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                //选择左边完成
                if (mOnChoiceListener != null)
                    mOnChoiceListener.onChoiceButton(ButtonEnum.LEFT);
            }
        });
        mRightToLeftAnimator.start();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_left) {
            rightToLeft();
        } else if (v.getId() == R.id.tv_right) {
            leftToRight();
        }
    }

    private OnChoiceListener mOnChoiceListener;

    public interface OnChoiceListener {
        void onChoiceButton(ButtonEnum buttonEnum);
    }

    public void setOnChoiceListener(OnChoiceListener onChoiceListener) {
        mOnChoiceListener = onChoiceListener;
    }

    public enum ButtonEnum {
        LEFT, RIGHT
    }

    /**
     * 获取当前选择的按钮位置
     *
     * @return
     */
    public ButtonEnum getChoiceButton() {
        if (mIsSelectLeft)
            return ButtonEnum.LEFT;
        else
            return ButtonEnum.RIGHT;
    }

    public void leftToRightQuick() {
        if (!mIsSelectLeft) {
            return;
        }
        mIsSelectLeft = false;
        if (mLeftToRightAnimator != null && mLeftToRightAnimator.isRunning()) {
            mLeftToRightAnimator.end();
        }

        mLeftToRightAnimator = ValueAnimator.ofInt(getPaddingLeft(), getWidth() / 2).setDuration(50);
        mLeftToRightAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            llBg.setTranslationX(value);
        });
        mLeftToRightAnimator.start();
    }

    public void rightToLeftQuick() {
        if (mIsSelectLeft) {
            return;
        }
        mIsSelectLeft = true;

        if (mRightToLeftAnimator != null && mRightToLeftAnimator.isRunning()) {
            mRightToLeftAnimator.end();
        }

        mRightToLeftAnimator = ValueAnimator.ofInt(getWidth() / 2, getPaddingLeft()).setDuration(50);
        mRightToLeftAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            llBg.setTranslationX(value);
        });
        mRightToLeftAnimator.start();
    }
}
